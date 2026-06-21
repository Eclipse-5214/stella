package co.stellarskys.stella.api.lumina.renderer.vk

import co.stellarskys.stella.mixins.accessors.AccessorGpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vulkan.VulkanDevice
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.util.vma.Vma.*
import org.lwjgl.util.vma.VmaAllocationCreateInfo
import org.lwjgl.util.vma.VmaAllocationInfo
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import java.nio.ByteBuffer

internal object VKUtils {
    lateinit var device: VkDevice
    lateinit var queue: VkQueue
    lateinit var mcVkDevice: VulkanDevice
    var queueFamily: Int = -1
    var vma: Long = 0L

    var commandPool: Long = VK_NULL_HANDLE
    var descriptorPool: Long = VK_NULL_HANDLE

    fun init() {
        val gpuDevice = RenderSystem.getDevice() as AccessorGpuDevice
        mcVkDevice = gpuDevice.backend as VulkanDevice
        device = mcVkDevice.vkDevice()
        queue = mcVkDevice.graphicsQueue().vkQueue()
        queueFamily = mcVkDevice.graphicsQueue().queueFamilyIndex()
        vma = mcVkDevice.vma()

        createCommandPool()
        createDescriptorPool()
    }

    data class VmaBuffer(val buffer: Long, val allocation: Long, val mappedPtr: Long)

    fun createStagingBuffer(size: Long): VmaBuffer {
        MemoryStack.stackPush().use { stack ->
            val bufInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(VK_BUFFER_USAGE_TRANSFER_SRC_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            val allocInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(VMA_MEMORY_USAGE_AUTO)
                .flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or VMA_ALLOCATION_CREATE_MAPPED_BIT)
            val pBuffer = stack.mallocLong(1)
            val pAlloc = stack.mallocPointer(1)
            val pAllocInfo = VmaAllocationInfo.calloc(stack)
            check(vmaCreateBuffer(vma, bufInfo, allocInfo, pBuffer, pAlloc, pAllocInfo) == VK_SUCCESS)
            return VmaBuffer(pBuffer[0], pAlloc[0], pAllocInfo.pMappedData())
        }
    }

    fun createHostVertexBuffer(size: Long): VmaBuffer {
        MemoryStack.stackPush().use { stack ->
            val bufInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(VK_BUFFER_USAGE_VERTEX_BUFFER_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            val allocInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(VMA_MEMORY_USAGE_AUTO)
                .flags(VMA_ALLOCATION_CREATE_HOST_ACCESS_SEQUENTIAL_WRITE_BIT or VMA_ALLOCATION_CREATE_MAPPED_BIT)
            val pBuffer = stack.mallocLong(1)
            val pAlloc = stack.mallocPointer(1)
            val pAllocInfo = VmaAllocationInfo.calloc(stack)
            check(vmaCreateBuffer(vma, bufInfo, allocInfo, pBuffer, pAlloc, pAllocInfo) == VK_SUCCESS)
            return VmaBuffer(pBuffer[0], pAlloc[0], pAllocInfo.pMappedData())
        }
    }

    fun createDeviceBuffer(size: Long, usage: Int): VmaBuffer {
        MemoryStack.stackPush().use { stack ->
            val bufInfo = VkBufferCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                .size(size)
                .usage(usage or VK_BUFFER_USAGE_TRANSFER_DST_BIT)
                .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
            val allocInfo = VmaAllocationCreateInfo.calloc(stack)
                .usage(VMA_MEMORY_USAGE_AUTO)
                .requiredFlags(VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT)
            val pBuffer = stack.mallocLong(1)
            val pAlloc = stack.mallocPointer(1)
            check(vmaCreateBuffer(vma, bufInfo, allocInfo, pBuffer, pAlloc, null) == VK_SUCCESS)
            return VmaBuffer(pBuffer[0], pAlloc[0], 0L)
        }
    }

    fun destroyBuffer(buf: VmaBuffer) {
        vmaDestroyBuffer(vma, buf.buffer, buf.allocation)
    }

    fun runOneShot(block: (VkCommandBuffer) -> Unit) {
        MemoryStack.stackPush().use { stack ->
            val allocInfo = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)
            val pBuf = stack.mallocPointer(1)
            check(vkAllocateCommandBuffers(device, allocInfo, pBuf) == VK_SUCCESS)
            val cmd = VkCommandBuffer(pBuf[0], device)

            val beginInfo = VkCommandBufferBeginInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
            vkBeginCommandBuffer(cmd, beginInfo)
            block(cmd)
            vkEndCommandBuffer(cmd)

            val submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(cmd))
            vkQueueSubmit(queue, submitInfo, VK_NULL_HANDLE)
            vkQueueWaitIdle(queue)
            vkFreeCommandBuffers(device, commandPool, cmd)
        }
    }

    private fun createCommandPool() {
        MemoryStack.stackPush().use { stack ->
            val info = VkCommandPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT)
                .queueFamilyIndex(queueFamily)
            val pPool = stack.mallocLong(1)
            check(vkCreateCommandPool(device, info, null, pPool) == VK_SUCCESS)
            commandPool = pPool[0]
        }
    }

    private fun createDescriptorPool() {
        MemoryStack.stackPush().use { stack ->
            val sizes = VkDescriptorPoolSize.calloc(1, stack)
            sizes[0].type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(128)
            val info = VkDescriptorPoolCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                .flags(VK_DESCRIPTOR_POOL_CREATE_FREE_DESCRIPTOR_SET_BIT)
                .maxSets(128)
                .pPoolSizes(sizes)
            val pPool = stack.mallocLong(1)
            check(vkCreateDescriptorPool(device, info, null, pPool) == VK_SUCCESS)
            descriptorPool = pPool[0]
        }
    }

    fun allocateCommandBuffer(): VkCommandBuffer {
        MemoryStack.stackPush().use { stack ->
            val info = VkCommandBufferAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                .commandPool(commandPool)
                .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                .commandBufferCount(1)
            val pBuf = stack.mallocPointer(1)
            check(vkAllocateCommandBuffers(device, info, pBuf) == VK_SUCCESS)
            return VkCommandBuffer(pBuf[0], device)
        }
    }

    fun unpackPremultiplied(argb: Int): FloatArray {
        val a = ((argb ushr 24) and 0xFF) / 255f
        val r = ((argb ushr 16) and 0xFF) / 255f
        val g = ((argb ushr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return floatArrayOf(r * a, g * a, b * a, a)
    }

    fun orthoProjection(w: Int, h: Int): FloatArray {
        // Vulkan clip space: Y points down (matches 2D coords), Z range [0,1]
        return floatArrayOf(
            2f / w, 0f,     0f, 0f,
            0f,     2f / h, 0f, 0f,
            0f,     0f,     1f, 0f,
            -1f,   -1f,     0f, 1f
        )
    }

    fun destroy() {
        if (descriptorPool != VK_NULL_HANDLE) {
            vkDestroyDescriptorPool(device, descriptorPool, null)
            descriptorPool = VK_NULL_HANDLE
        }
        if (commandPool != VK_NULL_HANDLE) {
            vkDestroyCommandPool(device, commandPool, null)
            commandPool = VK_NULL_HANDLE
        }
    }
}