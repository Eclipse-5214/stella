package co.stellarskys.stella.api.lumina.renderer.vk

import co.stellarskys.stella.Stella
import net.minecraft.resources.Identifier
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import java.nio.ByteBuffer

internal object VKPipelineManager {
    var shapePipeline: Long = VK_NULL_HANDLE
    var shapePipelineMasked: Long = VK_NULL_HANDLE
    var texturePipeline: Long = VK_NULL_HANDLE

    var shapePipelineLayout: Long = VK_NULL_HANDLE
    var texturePipelineLayout: Long = VK_NULL_HANDLE
    var textureDescSetLayout: Long = VK_NULL_HANDLE

    var renderPass: Long = VK_NULL_HANDLE
    private var currentRenderPassFormat: Int = 0

    private var shapeVert: Long = VK_NULL_HANDLE
    private var shapeFrag: Long = VK_NULL_HANDLE
    private var texVert: Long = VK_NULL_HANDLE
    private var texFrag: Long = VK_NULL_HANDLE

    fun init() {
        loadShaderModules()
        createRenderPass(VK_FORMAT_R8G8B8A8_UNORM)
        createDescriptorSetLayout()
        createShapePipeline()
        createTexturePipeline()
    }

    private fun loadShaderModules() {
        shapeVert = createShaderModule(loadSpirv("shaders/vk/lumina_shape_vert.spv"))
        shapeFrag = createShaderModule(loadSpirv("shaders/vk/lumina_shape_frag.spv"))
        texVert = createShaderModule(loadSpirv("shaders/vk/lumina_tex_vert.spv"))
        texFrag = createShaderModule(loadSpirv("shaders/vk/lumina_tex_frag.spv"))
    }

    fun ensureRenderPass(format: Int) {
        if (format == currentRenderPassFormat && renderPass != VK_NULL_HANDLE) return
        // Destroy old pipelines + render pass, recreate with new format
        val d = VKUtils.device
        if (shapePipeline != VK_NULL_HANDLE) { vkDestroyPipeline(d, shapePipeline, null); shapePipeline = VK_NULL_HANDLE }
        if (shapePipelineMasked != VK_NULL_HANDLE) { vkDestroyPipeline(d, shapePipelineMasked, null); shapePipelineMasked = VK_NULL_HANDLE }
        if (texturePipeline != VK_NULL_HANDLE) { vkDestroyPipeline(d, texturePipeline, null); texturePipeline = VK_NULL_HANDLE }
        if (renderPass != VK_NULL_HANDLE) { vkDestroyRenderPass(d, renderPass, null); renderPass = VK_NULL_HANDLE }
        createRenderPass(format)
        createShapePipeline()
        createTexturePipeline()
    }

    private fun createRenderPass(format: Int) {
        MemoryStack.stackPush().use { stack ->
            val colorAttachment = VkAttachmentDescription.calloc(1, stack)
            colorAttachment[0]
                .format(format)
                .samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_LOAD)
                .storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
                .finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            val colorRef = VkAttachmentReference.calloc(1, stack)
            colorRef[0].attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)

            val subpass = VkSubpassDescription.calloc(1, stack)
            subpass[0]
                .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                .colorAttachmentCount(1)
                .pColorAttachments(colorRef)

            val rpInfo = VkRenderPassCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(colorAttachment)
                .pSubpasses(subpass)

            val pRenderPass = stack.mallocLong(1)
            check(vkCreateRenderPass(VKUtils.device, rpInfo, null, pRenderPass) == VK_SUCCESS)
            renderPass = pRenderPass[0]
            currentRenderPassFormat = format
        }
    }

    private fun createShapePipeline() {
        // Vertex input:
        //   binding 0, stride = 28 bytes (7 floats), rate = VERTEX
        //   attr 0: offset 0,  R32G32_SFLOAT        (position)
        //   attr 1: offset 8,  R32G32B32A32_SFLOAT   (color)
        //   attr 2: offset 24, R32_SFLOAT             (coverage)
        //
        // Push constants: mat4 (64 bytes)
        // Blend: ONE, ONE_MINUS_SRC_ALPHA (premultiplied)
        // Dynamic state: viewport, scissor

        shapePipelineLayout = createPipelineLayout(
            descriptorSetLayouts = longArrayOf(),
            pushConstantSize = 64  // mat4
        )

        shapePipeline = buildGraphicsPipeline(
            layout = shapePipelineLayout,
            vertModule = shapeVert, fragModule = shapeFrag,
            bindingStride = 7 * 4,
            attributes = listOf(
                Attr(0, VK_FORMAT_R32G32_SFLOAT, 0),
                Attr(1, VK_FORMAT_R32G32B32A32_SFLOAT, 8),
                Attr(2, VK_FORMAT_R32_SFLOAT, 24)
            ),
            srcBlend = VK_BLEND_FACTOR_ONE,
            dstBlend = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA
        )

        // Masked variant: DST_ALPHA, ZERO (for drawMasked content)
        shapePipelineMasked = buildGraphicsPipeline(
            layout = shapePipelineLayout,
            vertModule = shapeVert, fragModule = shapeFrag,
            bindingStride = 7 * 4,
            attributes = listOf(
                Attr(0, VK_FORMAT_R32G32_SFLOAT, 0),
                Attr(1, VK_FORMAT_R32G32B32A32_SFLOAT, 8),
                Attr(2, VK_FORMAT_R32_SFLOAT, 24)
            ),
            srcBlend = VK_BLEND_FACTOR_DST_ALPHA,
            dstBlend = VK_BLEND_FACTOR_ZERO
        )
    }

    private fun createDescriptorSetLayout() {
        // One combined image sampler at binding 0
        MemoryStack.stackPush().use { stack ->
            val binding = VkDescriptorSetLayoutBinding.calloc(1, stack)
            binding[0]
                .binding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(1)
                .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)

            val info = VkDescriptorSetLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                .pBindings(binding)

            val pLayout = stack.mallocLong(1)
            check(vkCreateDescriptorSetLayout(VKUtils.device, info, null, pLayout) == VK_SUCCESS)
            textureDescSetLayout = pLayout[0]
        }
    }

    private fun createTexturePipeline() {
        // Vertex input:
        //   binding 0, stride = 32 bytes (8 floats), rate = VERTEX
        //   attr 0: offset 0,  R32G32_SFLOAT        (position)
        //   attr 1: offset 8,  R32G32_SFLOAT        (uv)
        //   attr 2: offset 16, R32G32B32A32_SFLOAT   (color)
        //
        // Push constants: mat4 + int = 68 bytes
        // Descriptor set 0: combined image sampler
        // Blend: ONE, ONE_MINUS_SRC_ALPHA

        texturePipelineLayout = createPipelineLayout(
            descriptorSetLayouts = longArrayOf(textureDescSetLayout),
            pushConstantSize = 68  // mat4 (64) + int (4)
        )

        texturePipeline = buildGraphicsPipeline(
            layout = texturePipelineLayout,
            vertModule = texVert, fragModule = texFrag,
            bindingStride = 8 * 4,
            attributes = listOf(
                Attr(0, VK_FORMAT_R32G32_SFLOAT, 0),
                Attr(1, VK_FORMAT_R32G32_SFLOAT, 8),
                Attr(2, VK_FORMAT_R32G32B32A32_SFLOAT, 16)
            ),
            srcBlend = VK_BLEND_FACTOR_ONE,
            dstBlend = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA
        )
    }

    // --- Helper functions you'd implement ---

    data class Attr(val location: Int, val format: Int, val offset: Int)

    private fun createPipelineLayout(descriptorSetLayouts: LongArray, pushConstantSize: Int): Long {
        MemoryStack.stackPush().use { stack ->
            val pushRange = VkPushConstantRange.calloc(1, stack)
            pushRange[0]
                .stageFlags(VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT)
                .offset(0)
                .size(pushConstantSize)

            val pLayouts = if (descriptorSetLayouts.isNotEmpty()) {
                val buf = stack.mallocLong(descriptorSetLayouts.size)
                descriptorSetLayouts.forEach { buf.put(it) }
                buf.flip()
            } else null

            val info = VkPipelineLayoutCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                .pPushConstantRanges(pushRange)
            if (pLayouts != null) info.pSetLayouts(pLayouts)

            val pLayout = stack.mallocLong(1)
            check(vkCreatePipelineLayout(VKUtils.device, info, null, pLayout) == VK_SUCCESS)
            return pLayout[0]
        }
    }

    private fun buildGraphicsPipeline(
        layout: Long, vertModule: Long, fragModule: Long,
        bindingStride: Int, attributes: List<Attr>,
        srcBlend: Int, dstBlend: Int
    ): Long {
        MemoryStack.stackPush().use { stack ->
            // Shader stages
            val stages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
            stages[0]
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_VERTEX_BIT)
                .module(vertModule)
                .pName(stack.UTF8("main"))
            stages[1]
                .sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                .stage(VK_SHADER_STAGE_FRAGMENT_BIT)
                .module(fragModule)
                .pName(stack.UTF8("main"))

            // Vertex input
            val bindingDesc = VkVertexInputBindingDescription.calloc(1, stack)
            bindingDesc[0].binding(0).stride(bindingStride).inputRate(VK_VERTEX_INPUT_RATE_VERTEX)

            val attrDescs = VkVertexInputAttributeDescription.calloc(attributes.size, stack)
            attributes.forEachIndexed { i, attr ->
                attrDescs[i].location(attr.location).binding(0).format(attr.format).offset(attr.offset)
            }

            val vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(bindingDesc)
                .pVertexAttributeDescriptions(attrDescs)

            // Input assembly
            val inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                .primitiveRestartEnable(false)

            // Viewport (dynamic — we set it at draw time)
            val viewportState = VkPipelineViewportStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1)
                .scissorCount(1)

            // Rasterization
            val raster = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .polygonMode(VK_POLYGON_MODE_FILL)
                .cullMode(VK_CULL_MODE_NONE)
                .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                .lineWidth(1f)

            // Multisample
            val multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

            // Color blend
            val blendAttachment = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            blendAttachment[0]
                .blendEnable(true)
                .srcColorBlendFactor(srcBlend)
                .dstColorBlendFactor(dstBlend)
                .colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(srcBlend)
                .dstAlphaBlendFactor(dstBlend)
                .alphaBlendOp(VK_BLEND_OP_ADD)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)

            val colorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                .pAttachments(blendAttachment)

            // Dynamic state
            val dynamicStates = stack.mallocInt(2)
            dynamicStates.put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip()
            val dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO)
                .pDynamicStates(dynamicStates)

            // Pipeline
            val pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            pipelineInfo[0]
                .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .pStages(stages)
                .pVertexInputState(vertexInput)
                .pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState)
                .pRasterizationState(raster)
                .pMultisampleState(multisample)
                .pColorBlendState(colorBlend)
                .pDynamicState(dynamicState)
                .layout(layout)
                .renderPass(renderPass)
                .subpass(0)

            val pPipeline = stack.mallocLong(1)
            check(vkCreateGraphicsPipelines(VKUtils.device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline) == VK_SUCCESS)
            return pPipeline[0]
        }
    }

    private fun createShaderModule(spirvBytes: ByteBuffer): Long {
        MemoryStack.stackPush().use { stack ->
            val info = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(spirvBytes)
            val pModule = stack.mallocLong(1)
            check(vkCreateShaderModule(VKUtils.device, info, null, pModule) == VK_SUCCESS)
            return pModule[0]
        }
    }

    private fun loadSpirv(path: String): ByteBuffer {
        val id = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, path)
        val bytes = net.minecraft.client.Minecraft.getInstance().resourceManager.getResource(id)
            .orElseThrow { RuntimeException("Missing SPIR-V shader: $id") }
            .open().readBytes()
        val buf = MemoryUtil.memAlloc(bytes.size)
        buf.put(bytes).flip()
        return buf
    }

    fun destroy() {
        val d = VKUtils.device
        vkDestroyPipeline(d, shapePipeline, null)
        vkDestroyPipeline(d, shapePipelineMasked, null)
        vkDestroyPipeline(d, texturePipeline, null)
        vkDestroyPipelineLayout(d, shapePipelineLayout, null)
        vkDestroyPipelineLayout(d, texturePipelineLayout, null)
        vkDestroyDescriptorSetLayout(d, textureDescSetLayout, null)
        vkDestroyShaderModule(d, shapeVert, null)
        vkDestroyShaderModule(d, shapeFrag, null)
        vkDestroyShaderModule(d, texVert, null)
        vkDestroyShaderModule(d, texFrag, null)
        vkDestroyRenderPass(d, renderPass, null)
    }
}