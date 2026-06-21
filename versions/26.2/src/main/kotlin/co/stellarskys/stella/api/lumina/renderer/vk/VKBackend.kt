package co.stellarskys.stella.api.lumina.renderer.vk

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
    private var frameCount = 0

    data class VkTextureHandle(
        val image: Long,
        val allocation: Long,
        val imageView: Long,
        val sampler: Long
    )
    private val textures = mutableMapOf<Int, VkTextureHandle>()
    private var nextTexId = 1

    private var commandBuffer: VkCommandBuffer? = null
    private var fence: Long = VK_NULL_HANDLE

    private var framebuffer: Long = VK_NULL_HANDLE
    private var targetImageView: Long = VK_NULL_HANDLE
    private var lastTargetImage: Long = VK_NULL_HANDLE
    private var renderPassActive = false
    private var fbWidth = 0
    private var fbHeight = 0

    fun beginRenderPassIfNeeded() {
        if (renderPassActive) return
        val cmd = commandBuffer ?: return
        if (frameCount <= 5) logger.info("Beginning render pass: fb={}, {}x{}", framebuffer, fbWidth, fbHeight)

        // Transition image to COLOR_ATTACHMENT_OPTIMAL (MC may have it in any layout)
        MemoryStack.stackPush().use { stack ->
            val barrier = VkImageMemoryBarrier.calloc(1, stack)
            barrier[0]
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                .newLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(lastTargetImage)
            barrier[0].subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1)
            barrier[0].srcAccessMask(0)
                .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT)
            vkCmdPipelineBarrier(cmd,
                VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT,
                0, null, null, barrier)
        }

        MemoryStack.stackPush().use { stack ->
            val rpBeginInfo = VkRenderPassBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                .renderPass(VKPipelineManager.renderPass)
                .framebuffer(framebuffer)
            rpBeginInfo.renderArea().offset().x(0).y(0)
            rpBeginInfo.renderArea().extent().width(fbWidth).height(fbHeight)
            vkCmdBeginRenderPass(cmd, rpBeginInfo, VK_SUBPASS_CONTENTS_INLINE)
        }
        renderPassActive = true
    }

    fun ensureInit() {
        if (initialized) return
        VKUtils.init()
        logger.info("VKUtils initialized: device={}, queue={}, vma={}", VKUtils.device, VKUtils.queue, VKUtils.vma)
        VKPipelineManager.init()
        logger.info("Pipelines created: shape={}, masked={}, tex={}, renderPass={}",
            VKPipelineManager.shapePipeline, VKPipelineManager.shapePipelineMasked,
            VKPipelineManager.texturePipeline, VKPipelineManager.renderPass)
        VKShapeRenderer.init()
        VKTextureRenderer.init()
        createFence()
        commandBuffer = VKUtils.allocateCommandBuffer()
        initialized = true
        logger.info("VKBackend fully initialized")
    }

    private fun createFence() {
        MemoryStack.stackPush().use { stack ->
            val info = VkFenceCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                .flags(VK_FENCE_CREATE_SIGNALED_BIT)
            val pFence = stack.mallocLong(1)
            check(vkCreateFence(VKUtils.device, info, null, pFence) == VK_SUCCESS)
            fence = pFence[0]
        }
    }

    override fun renderShapes(shapes: List<Lumina.QueuedShape>, vw: Int, vh: Int) {
        val cmd = commandBuffer ?: return
        if (frameCount < 5) logger.info("renderShapes: {} shapes, viewport={}x{}", shapes.size, vw, vh)
        VKShapeRenderer.render(cmd, shapes, vw, vh)
    }

    override fun renderTextured(text: List<LuminaBackend.TextEntry>,
                                images: List<LuminaBackend.ImageEntry>, vw: Int, vh: Int) {
        val cmd = commandBuffer ?: return
        if (frameCount < 5) logger.info("renderTextured: {} text, {} images, viewport={}x{}", text.size, images.size, vw, vh)
        VKTextureRenderer.render(cmd, text, images, vw, vh)
    }

    override fun uploadTexture(width: Int, height: Int, data: ByteBuffer,
                               format: LuminaBackend.TextureFormat, mipmap: Boolean): Int {
        ensureInit()
        val vkFormat = when (format) {
            LuminaBackend.TextureFormat.RGBA -> VK_FORMAT_R8G8B8A8_UNORM
            LuminaBackend.TextureFormat.R8 -> VK_FORMAT_R8_UNORM
        }
        val bytesPerPixel = when (format) {
            LuminaBackend.TextureFormat.RGBA -> 4
            LuminaBackend.TextureFormat.R8 -> 1
        }
        val imageSize = width.toLong() * height * bytesPerPixel

        MemoryStack.stackPush().use { stack ->
            // Create image
            val imageInfo = VkImageCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                .imageType(VK_IMAGE_TYPE_2D)
                .format(vkFormat)
                .mipLevels(if (mipmap) calculateMipLevels(width, height) else 1)
                .arrayLayers(1)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .tiling(VK_IMAGE_TILING_OPTIMAL)
                .usage(VK_IMAGE_USAGE_SAMPLED_BIT or VK_IMAGE_USAGE_TRANSFER_DST_BIT or
                        if (mipmap) VK_IMAGE_USAGE_TRANSFER_SRC_BIT else 0)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
            imageInfo.extent().width(width).height(height).depth(1)

            val allocInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(VMA_MEMORY_USAGE_AUTO)
                .requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)

            val pImage = stack.mallocLong(1)
            val pAlloc = stack.mallocPointer(1)
            check(vmaCreateImage(VKUtils.vma, imageInfo, allocInfo, pImage, pAlloc, null) == VK_SUCCESS)
            val image = pImage[0]
            val alloc = pAlloc[0]

            // Upload via staging buffer
            val staging = VKUtils.createStagingBuffer(imageSize)
            MemoryUtil.memCopy(MemoryUtil.memAddress(data), staging.mappedPtr, imageSize)

            VKUtils.runOneShot { cmd ->
                // Transition: UNDEFINED → TRANSFER_DST
                transitionImageLayout(cmd, image, VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL)

                // Copy staging → image
                val region = VkBufferImageCopy.calloc(1, stack)
                region[0].bufferOffset(0).bufferRowLength(0).bufferImageHeight(0)
                region[0].imageSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1)
                region[0].imageOffset().set(0, 0, 0)
                region[0].imageExtent().set(width, height, 1)
                vkCmdCopyBufferToImage(cmd, staging.buffer, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region)

                // Transition: TRANSFER_DST → SHADER_READ_ONLY
                transitionImageLayout(cmd, image, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL)
            }
            VKUtils.destroyBuffer(staging)

            // Create image view
            val viewInfo = VkImageViewCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                .image(image)
                .viewType(VK_IMAGE_VIEW_TYPE_2D)
                .format(vkFormat)
            viewInfo.subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1)
            val pView = stack.mallocLong(1)
            check(vkCreateImageView(VKUtils.device, viewInfo, null, pView) == VK_SUCCESS)
            val view = pView[0]

            // Create sampler
            val samplerInfo = VkSamplerCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                .magFilter(VK_FILTER_LINEAR)
                .minFilter(VK_FILTER_LINEAR)
                .mipmapMode(if (mipmap) VK_SAMPLER_MIPMAP_MODE_LINEAR else VK_SAMPLER_MIPMAP_MODE_NEAREST)
                .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE)
                .maxLod(if (mipmap) VK_LOD_CLAMP_NONE else 0f)
            val pSampler = stack.mallocLong(1)
            check(vkCreateSampler(VKUtils.device, samplerInfo, null, pSampler) == VK_SUCCESS)
            val sampler = pSampler[0]

            val handle = VkTextureHandle(image, alloc, view, sampler)
            val id = nextTexId++
            textures[id] = handle
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
        frameCount++

        // Get the actual format of the PIP target texture
        val colorTexView = RenderSystem.outputColorTextureOverride
        val vkFormat = if (colorTexView != null) {
            VulkanConst.toVk(colorTexView.texture().format)
        } else {
            VK_FORMAT_R8G8B8A8_UNORM
        }

        if (frameCount <= 5) logger.info("setupRenderTarget: image=0x{}, {}x{}, format={}",
            java.lang.Long.toHexString(targetId), width, height, vkFormat)

        // Recreate render pass + framebuffer if target changed
        if (targetId != lastTargetImage || fbWidth != width || fbHeight != height || framebuffer == VK_NULL_HANDLE) {
            if (targetImageView != VK_NULL_HANDLE) vkDestroyImageView(VKUtils.device, targetImageView, null)
            if (framebuffer != VK_NULL_HANDLE) vkDestroyFramebuffer(VKUtils.device, framebuffer, null)

            // Recreate render pass if format changed
            VKPipelineManager.ensureRenderPass(vkFormat)

            MemoryStack.stackPush().use { stack ->
                val viewInfo = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(targetId)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(vkFormat)
                viewInfo.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1)
                val pView = stack.mallocLong(1)
                check(vkCreateImageView(VKUtils.device, viewInfo, null, pView) == VK_SUCCESS)
                targetImageView = pView[0]

                val fbInfo = VkFramebufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                    .renderPass(VKPipelineManager.renderPass)
                    .pAttachments(stack.longs(targetImageView))
                    .width(width).height(height).layers(1)
                val pFb = stack.mallocLong(1)
                check(vkCreateFramebuffer(VKUtils.device, fbInfo, null, pFb) == VK_SUCCESS)
                framebuffer = pFb[0]
            }
            lastTargetImage = targetId; fbWidth = width; fbHeight = height
        }

        // Begin command buffer (render pass is started later, before draws)
        val cmd = commandBuffer!!
        vkWaitForFences(VKUtils.device, fence, true, Long.MAX_VALUE)
        vkResetFences(VKUtils.device, fence)
        vkResetCommandBuffer(cmd, 0)

        MemoryStack.stackPush().use { stack ->
            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            vkBeginCommandBuffer(cmd, beginInfo)
        }
        renderPassActive = false
    }

    override fun resetAfterRender() {
        val cmd = commandBuffer ?: return
        if (frameCount <= 5) logger.info("resetAfterRender: renderPassWasActive={}", renderPassActive)
        if (renderPassActive) {
            vkCmdEndRenderPass(cmd)
            renderPassActive = false
            transitionImageLayout(cmd, lastTargetImage, VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL, VK_IMAGE_LAYOUT_GENERAL)
        }
        vkEndCommandBuffer(cmd)

        MemoryStack.stackPush().use { stack ->
            val submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(cmd))
            vkQueueSubmit(VKUtils.queue, submitInfo, fence)
        }
        // Wait for our commands to complete before MC reads the PIP texture
        vkQueueWaitIdle(VKUtils.queue)
    }

    override fun destroy() {
        if (!initialized) return
        vkDeviceWaitIdle(VKUtils.device)
        textures.keys.toList().forEach { deleteTexture(it) }
        if (targetImageView != VK_NULL_HANDLE) vkDestroyImageView(VKUtils.device, targetImageView, null)
        if (framebuffer != VK_NULL_HANDLE) vkDestroyFramebuffer(VKUtils.device, framebuffer, null)
        VKTextureRenderer.destroy()
        VKShapeRenderer.destroy()
        VKPipelineManager.destroy()
        vkDestroyFence(VKUtils.device, fence, null)
        VKUtils.destroy()
        initialized = false
    }

    fun getTextureHandle(id: Int): VkTextureHandle = textures[id]!!

    private fun calculateMipLevels(w: Int, h: Int): Int {
        var levels = 1; var size = maxOf(w, h)
        while (size > 1) { size /= 2; levels++ }
        return levels
    }

    private fun transitionImageLayout(cmd: VkCommandBuffer, image: Long, oldLayout: Int, newLayout: Int) {
        MemoryStack.stackPush().use { stack ->
            val barrier = VkImageMemoryBarrier.calloc(1, stack)
            barrier[0]
                .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                .oldLayout(oldLayout)
                .newLayout(newLayout)
                .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                .image(image)
            barrier[0].subresourceRange()
                .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1)

            val srcStage: Int; val dstStage: Int
            when {
                oldLayout == VK_IMAGE_LAYOUT_UNDEFINED && newLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL -> {
                    barrier[0].srcAccessMask(0).dstAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
                    srcStage = VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT
                    dstStage = VK_PIPELINE_STAGE_TRANSFER_BIT
                }
                oldLayout == VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_GENERAL -> {
                    barrier[0].srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    srcStage = VK_PIPELINE_STAGE_TRANSFER_BIT
                    dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                }
                oldLayout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL && newLayout == VK_IMAGE_LAYOUT_GENERAL -> {
                    barrier[0].srcAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT).dstAccessMask(VK_ACCESS_SHADER_READ_BIT)
                    srcStage = VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT
                    dstStage = VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT
                }
                else -> throw IllegalArgumentException("Unsupported layout transition: $oldLayout -> $newLayout")
            }
            vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, null, null, barrier)
        }
    }
}
