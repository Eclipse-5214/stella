package co.stellarskys.stella.api.lumina.renderer.vk

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vulkan.VulkanConst
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import java.nio.ByteBuffer

object VKBackend : LuminaBackend {
    private val logger = org.apache.logging.log4j.LogManager.getLogger("VKBackend")
    private var initialized = false
    private val textures = mutableMapOf<Int, VkTextureHandle>()
    private var nextTexId = 1
    private var commandBuffer: VkCommandBuffer? = null
    private var lastResetFrame = -1L
    private var framebuffer: Long = VK_NULL_HANDLE
    private var targetImageView: Long = VK_NULL_HANDLE
    private var lastTargetImage: Long = VK_NULL_HANDLE
    private var renderPassActive = false
    private var fbWidth = 0
    private var fbHeight = 0

    data class VkTextureHandle(val image: Long, val allocation: Long, val imageView: Long, val sampler: Long)

    fun beginRenderPassIfNeeded() {
        if (renderPassActive) return
        val cmd = commandBuffer ?: return
        MemoryStack.stackPush().use { stack ->
            val barrier = VkImageMemoryBarrier.calloc(1, stack)
            barrier[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(lastTargetImage).srcAccessMask(0).dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            barrier[0].subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1)
            vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT, 0, null, null, barrier)

            val rpInfo = VkRenderPassBeginInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(VKPipelineManager.renderPass).framebuffer(framebuffer)
            rpInfo.renderArea().offset().x(0).y(0)
            rpInfo.renderArea().extent().width(fbWidth).height(fbHeight)
            vkCmdBeginRenderPass(cmd, rpInfo, VK_SUBPASS_CONTENTS_INLINE)
        }
        renderPassActive = true
    }

    fun ensureInit() {
        if (initialized) return
        VKUtils.init(); VKPipelineManager.init(); VKShapeRenderer.init(); VKTextureRenderer.init()
        initialized = true
        logger.info("VKBackend initialized")
    }

    override fun renderShapes(shapes: List<Lumina.QueuedShape>, vw: Int, vh: Int) {
        VKShapeRenderer.render(commandBuffer ?: return, shapes, vw, vh)
    }

    override fun renderTextured(text: List<LuminaBackend.TextEntry>, images: List<LuminaBackend.ImageEntry>, vw: Int, vh: Int) {
        VKTextureRenderer.render(commandBuffer ?: return, text, images, vw, vh)
    }

    override fun uploadTexture(width: Int, height: Int, data: ByteBuffer, format: LuminaBackend.TextureFormat, mipmap: Boolean): Int {
        ensureInit()
        val vkFormat = when (format) { LuminaBackend.TextureFormat.RGBA -> VK_FORMAT_R8G8B8A8_UNORM; LuminaBackend.TextureFormat.R8 -> VK_FORMAT_R8_UNORM }
        val bpp = when (format) { LuminaBackend.TextureFormat.RGBA -> 4; LuminaBackend.TextureFormat.R8 -> 1 }
        val imageSize = width.toLong() * height * bpp
        val mipLevels = if (mipmap) calculateMipLevels(width, height) else 1

        MemoryStack.stackPush().use { stack ->
            val imageInfo = VkImageCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D).format(vkFormat).mipLevels(mipLevels).arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT).tiling(VK_IMAGE_TILING_OPTIMAL)
                .usage(VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT or if (mipmap) VK_IMAGE_USAGE_TRANSFER_SRC_BIT else 0)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE).initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            imageInfo.extent().width(width).height(height).depth(1)
            val allocInfo = VmaAllocationCreateInfo.calloc(stack).usage(VMA_MEMORY_USAGE_AUTO).requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
            val pImage = stack.mallocLong(1); val pAlloc = stack.mallocPointer(1)
            check(vmaCreateImage(VKUtils.vma, imageInfo, allocInfo, pImage, pAlloc, null) == VK_SUCCESS)
            val image = pImage[0]; val alloc = pAlloc[0]

            val staging = VKUtils.createStagingBuffer(imageSize)
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), staging.mappedPtr, imageSize)

            VKUtils.runOneShot { cmd ->
                MemoryStack.stackPush().use { s ->
                    val b = VkImageMemoryBarrier.calloc(1, s)
                    b[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                        .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED).newLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)
                        .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                        .image(image).srcAccessMask(0).dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    b[0].subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(mipLevels).baseArrayLayer(0).layerCount(1)
                    vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, b)
                }
                val region = VkBufferImageCopy.calloc(1, stack)
                region[0].bufferOffset(0).bufferRowLength(0).bufferImageHeight(0)
                region[0].imageSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(0).baseArrayLayer(0).layerCount(1)
                region[0].imageOffset().set(0, 0, 0); region[0].imageExtent().set(width, height, 1)
                vkCmdCopyBufferToImage(cmd, staging.buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)
                if (mipmap) generateMipmaps(cmd, image, width, height, mipLevels)
                else transitionImageLayout(cmd, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL)
            }
            VKUtils.destroyBuffer(staging)

            val viewInfo = VkImageViewCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image).viewType(VK_IMAGE_VIEW_TYPE_2D).format(vkFormat)
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(mipLevels).baseArrayLayer(0).layerCount(1)
            val pView = stack.mallocLong(1)
            check(vkCreateImageView(VKUtils.device, viewInfo, null, pView) == VK_SUCCESS)

            val samplerInfo = VkSamplerCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(VK_FILTER_LINEAR).minFilter(VK_FILTER_LINEAR)
                .mipmapMode(if (mipmap) VK_SAMPLER_MIPMAP_MODE_LINEAR else VK_SAMPLER_MIPMAP_MODE_NEAREST)
                .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE).addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE).maxLod(if (mipmap) VK_LOD_CLAMP_NONE else 0f)
            val pSampler = stack.mallocLong(1)
            check(vkCreateSampler(VKUtils.device, samplerInfo, null, pSampler) == VK_SUCCESS)

            val id = nextTexId++
            textures[id] = VkTextureHandle(image, alloc, pView[0], pSampler[0])
            return id
        }
    }

    override fun deleteTexture(id: Int) {
        val h = textures.remove(id) ?: return
        vkDestroySampler(VKUtils.device, h.sampler, null)
        vkDestroyImageView(VKUtils.device, h.imageView, null)
        vmaDestroyImage(VKUtils.vma, h.image, h.allocation)
    }

    override fun setupRenderTarget(targetId: Long, width: Int, height: Int) {
        ensureInit()
        val colorTexView = RenderSystem.outputColorTextureOverride
        val vkFormat = if (colorTexView != null) VulkanConst.toVk(colorTexView.texture().format) else VK_FORMAT_R8G8B8A8_UNORM
        lastTargetImage = targetId

        // Deferred destroy old framebuffer resources
        if (targetImageView != VK_NULL_HANDLE) {
            val oldView = targetImageView; val oldFb = framebuffer
            VKUtils.mcVkDevice.createCommandEncoder().queueForDestroy(com.mojang.blaze3d.vulkan.Destroyable {
                vkDestroyImageView(VKUtils.device, oldView, null)
                if (oldFb != VK_NULL_HANDLE) vkDestroyFramebuffer(VKUtils.device, oldFb, null)
            })
        }

        VKPipelineManager.ensureRenderPass(vkFormat)
        MemoryStack.stackPush().use { stack ->
            val viewInfo = VkImageViewCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(targetId).viewType(VK_IMAGE_VIEW_TYPE_2D).format(vkFormat)
            viewInfo.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1)
            val pView = stack.mallocLong(1)
            check(vkCreateImageView(VKUtils.device, viewInfo, null, pView) == VK_SUCCESS)
            targetImageView = pView[0]

            val fbInfo = VkFramebufferCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                .renderPass(VKPipelineManager.renderPass).pAttachments(stack.longs(targetImageView))
                .width(width).height(height).layers(1)
            val pFb = stack.mallocLong(1)
            check(vkCreateFramebuffer(VKUtils.device, fbInfo, null, pFb) == VK_SUCCESS)
            framebuffer = pFb[0]
        }
        fbWidth = width; fbHeight = height

        if (Stella.DELTA.frame != lastResetFrame) {
            VKShapeRenderer.resetFrame(); VKTextureRenderer.resetFrame()
            lastResetFrame = Stella.DELTA.frame
        }

        commandBuffer = VKUtils.mcVkDevice.createCommandEncoder().allocateAndBeginTransientCommandBuffer()
        renderPassActive = false
    }

    override fun resetAfterRender() {
        val cmd = commandBuffer ?: return
        if (renderPassActive) {
            vkCmdEndRenderPass(cmd)
            renderPassActive = false
            transitionImageLayout(cmd, lastTargetImage, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL)
        }
        vkEndCommandBuffer(cmd)
        VKUtils.mcVkDevice.createCommandEncoder().execute(cmd)
    }

    override fun destroy() {
        if (!initialized) return
        vkDeviceWaitIdle(VKUtils.device)
        textures.keys.toList().forEach { deleteTexture(it) }
        if (targetImageView != VK_NULL_HANDLE) vkDestroyImageView(VKUtils.device, targetImageView, null)
        if (framebuffer != VK_NULL_HANDLE) vkDestroyFramebuffer(VKUtils.device, framebuffer, null)
        VKTextureRenderer.destroy(); VKShapeRenderer.destroy(); VKPipelineManager.destroy(); VKUtils.destroy()
        initialized = false
    }

    fun getTextureHandle(id: Int): VkTextureHandle = textures[id]!!

    private fun generateMipmaps(cmd: VkCommandBuffer, image: Long, texWidth: Int, texHeight: Int, mipLevels: Int) {
        MemoryStack.stackPush().use { stack ->
            val barrier = VkImageMemoryBarrier.calloc(1, stack)
            barrier[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).image(image)
            barrier[0].subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).levelCount(1).baseArrayLayer(0).layerCount(1)
            var mipW = texWidth; var mipH = texHeight

            for (i in 1 until mipLevels) {
                barrier[0].subresourceRange().baseMipLevel(i - 1)
                barrier[0].oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL)
                    .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_TRANSFER_READ_BIT)
                vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT, 0, null, null, barrier)

                val blit = VkImageBlit.calloc(1, stack)
                blit[0].srcOffsets(0).set(0, 0, 0); blit[0].srcOffsets(1).set(mipW, mipH, 1)
                blit[0].srcSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(i - 1).baseArrayLayer(0).layerCount(1)
                val nextW = maxOf(1, mipW / 2); val nextH = maxOf(1, mipH / 2)
                blit[0].dstOffsets(0).set(0, 0, 0); blit[0].dstOffsets(1).set(nextW, nextH, 1)
                blit[0].dstSubresource().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).mipLevel(i).baseArrayLayer(0).layerCount(1)
                vkCmdBlitImage(cmd, image, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, blit, VK_FILTER_LINEAR)

                barrier[0].oldLayout(VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_GENERAL)
                    .srcAccessMask(VK_ACCESS_TRANSFER_READ_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier)
                mipW = nextW; mipH = nextH
            }
            barrier[0].subresourceRange().baseMipLevel(mipLevels - 1)
            barrier[0].oldLayout(VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL).newLayout(VK_IMAGE_LAYOUT_GENERAL)
                .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
            vkCmdPipelineBarrier(cmd, VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT, 0, null, null, barrier)
        }
    }

    private fun calculateMipLevels(w: Int, h: Int): Int {
        var levels = 1; var size = maxOf(w, h)
        while (size > 1) { size /= 2; levels++ }
        return levels
    }

    private fun transitionImageLayout(cmd: VkCommandBuffer, image: Long, oldLayout: Int, newLayout: Int) {
        MemoryStack.stackPush().use { stack ->
            val barrier = VkImageMemoryBarrier.calloc(1, stack)
            barrier[0].sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER).oldLayout(oldLayout).newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED).image(image)
            barrier[0].subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT).baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1)
            val srcStage: Int; val dstStage: Int
            when {
                oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                    barrier[0].srcAccessMask(0).dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT; dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT
                }
                oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_GENERAL -> {
                    barrier[0].srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT; dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                }
                oldLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_GENERAL -> {
                    barrier[0].srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    srcStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT; dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                }
                else -> throw IllegalArgumentException("Unsupported layout transition: $oldLayout -> $newLayout")
            }
            vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, null, null, barrier)
        }
    }
}
