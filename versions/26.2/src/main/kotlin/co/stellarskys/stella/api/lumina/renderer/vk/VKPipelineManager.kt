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

    data class Attr(val location: Int, val format: Int, val offset: Int)

    private val SHAPE_ATTRS = listOf(Attr(0, VK_FORMAT_R32G32_SFLOAT, 0), Attr(1, VK_FORMAT_R32G32B32A32_SFLOAT, 8), Attr(2, VK_FORMAT_R32_SFLOAT, 24))
    private val TEX_ATTRS = listOf(Attr(0, VK_FORMAT_R32G32_SFLOAT, 0), Attr(1, VK_FORMAT_R32G32_SFLOAT, 8), Attr(2, VK_FORMAT_R32G32B32A32_SFLOAT, 16))

    fun init() {
        shapeVert = createShaderModule(loadSpirv("shaders/vk/lumina_shape_vert.spv"))
        shapeFrag = createShaderModule(loadSpirv("shaders/vk/lumina_shape_frag.spv"))
        texVert = createShaderModule(loadSpirv("shaders/vk/lumina_tex_vert.spv"))
        texFrag = createShaderModule(loadSpirv("shaders/vk/lumina_tex_frag.spv"))
        createRenderPass(VK_FORMAT_R8G8B8A8_UNORM)
        createDescriptorSetLayout()
        createShapePipeline()
        createTexturePipeline()
    }

    fun ensureRenderPass(format: Int) {
        if (format == currentRenderPassFormat && renderPass != VK_NULL_HANDLE) return
        val d = VKUtils.device
        if (shapePipeline != VK_NULL_HANDLE) { vkDestroyPipeline(d, shapePipeline, null); shapePipeline = VK_NULL_HANDLE }
        if (shapePipelineMasked != VK_NULL_HANDLE) { vkDestroyPipeline(d, shapePipelineMasked, null); shapePipelineMasked = VK_NULL_HANDLE }
        if (texturePipeline != VK_NULL_HANDLE) { vkDestroyPipeline(d, texturePipeline, null); texturePipeline = VK_NULL_HANDLE }
        if (renderPass != VK_NULL_HANDLE) { vkDestroyRenderPass(d, renderPass, null); renderPass = VK_NULL_HANDLE }
        createRenderPass(format); createShapePipeline(); createTexturePipeline()
    }

    private fun createRenderPass(format: Int) {
        MemoryStack.stackPush().use { stack ->
            val attach = VkAttachmentDescription.calloc(1, stack)
            attach[0].format(format).samples(VK_SAMPLE_COUNT_1_BIT)
                .loadOp(VK_ATTACHMENT_LOAD_OP_LOAD).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE).stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                .initialLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL).finalLayout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            val ref = VkAttachmentReference.calloc(1, stack)
            ref[0].attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL)
            val subpass = VkSubpassDescription.calloc(1, stack)
            subpass[0].pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS).colorAttachmentCount(1).pColorAttachments(ref)
            val info = VkRenderPassCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                .pAttachments(attach).pSubpasses(subpass)
            val pRP = stack.mallocLong(1)
            check(vkCreateRenderPass(VKUtils.device, info, null, pRP) == VK_SUCCESS)
            renderPass = pRP[0]; currentRenderPassFormat = format
        }
    }

    private fun createShapePipeline() {
        shapePipelineLayout = createPipelineLayout(longArrayOf(), 64)
        shapePipeline = buildPipeline(shapePipelineLayout, shapeVert, shapeFrag, 7 * 4, SHAPE_ATTRS, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
        shapePipelineMasked = buildPipeline(shapePipelineLayout, shapeVert, shapeFrag, 7 * 4, SHAPE_ATTRS, VK_BLEND_FACTOR_DST_ALPHA, VK_BLEND_FACTOR_ZERO)
    }

    private fun createDescriptorSetLayout() {
        MemoryStack.stackPush().use { stack ->
            val binding = VkDescriptorSetLayoutBinding.calloc(1, stack)
            binding[0].binding(0).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).descriptorCount(1).stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT)
            val info = VkDescriptorSetLayoutCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO).pBindings(binding)
            val pLayout = stack.mallocLong(1)
            check(vkCreateDescriptorSetLayout(VKUtils.device, info, null, pLayout) == VK_SUCCESS)
            textureDescSetLayout = pLayout[0]
        }
    }

    private fun createTexturePipeline() {
        texturePipelineLayout = createPipelineLayout(longArrayOf(textureDescSetLayout), 68)
        texturePipeline = buildPipeline(texturePipelineLayout, texVert, texFrag, 8 * 4, TEX_ATTRS, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
    }

    private fun createPipelineLayout(descSetLayouts: LongArray, pushConstantSize: Int): Long {
        MemoryStack.stackPush().use { stack ->
            val pushRange = VkPushConstantRange.calloc(1, stack)
            pushRange[0].stageFlags(VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT).offset(0).size(pushConstantSize)
            val pLayouts = if (descSetLayouts.isNotEmpty()) stack.mallocLong(descSetLayouts.size).also { descSetLayouts.forEach { v -> it.put(v) } }.flip() else null
            val info = VkPipelineLayoutCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO).pPushConstantRanges(pushRange)
            if (pLayouts != null) info.pSetLayouts(pLayouts)
            val pLayout = stack.mallocLong(1)
            check(vkCreatePipelineLayout(VKUtils.device, info, null, pLayout) == VK_SUCCESS)
            return pLayout[0]
        }
    }

    private fun buildPipeline(layout: Long, vert: Long, frag: Long, stride: Int, attrs: List<Attr>, srcBlend: Int, dstBlend: Int): Long {
        MemoryStack.stackPush().use { stack ->
            val stages = VkPipelineShaderStageCreateInfo.calloc(2, stack)
            stages[0].sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK_SHADER_STAGE_VERTEX_BIT).module(vert).pName(stack.UTF8("main"))
            stages[1].sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO).stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(frag).pName(stack.UTF8("main"))

            val bindingDesc = VkVertexInputBindingDescription.calloc(1, stack)
            bindingDesc[0].binding(0).stride(stride).inputRate(VK_VERTEX_INPUT_RATE_VERTEX)
            val attrDescs = VkVertexInputAttributeDescription.calloc(attrs.size, stack)
            attrs.forEachIndexed { i, a -> attrDescs[i].location(a.location).binding(0).format(a.format).offset(a.offset) }

            val vertexInput = VkPipelineVertexInputStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                .pVertexBindingDescriptions(bindingDesc).pVertexAttributeDescriptions(attrDescs)
            val inputAssembly = VkPipelineInputAssemblyStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
            val viewportState = VkPipelineViewportStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                .viewportCount(1).scissorCount(1)
            val raster = VkPipelineRasterizationStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                .polygonMode(VK_POLYGON_MODE_FILL).cullMode(VK_CULL_MODE_NONE).frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE).lineWidth(1f)
            val multisample = VkPipelineMultisampleStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)

            val blendAttach = VkPipelineColorBlendAttachmentState.calloc(1, stack)
            blendAttach[0].blendEnable(true).srcColorBlendFactor(srcBlend).dstColorBlendFactor(dstBlend).colorBlendOp(VK_BLEND_OP_ADD)
                .srcAlphaBlendFactor(srcBlend).dstAlphaBlendFactor(dstBlend).alphaBlendOp(VK_BLEND_OP_ADD)
                .colorWriteMask(VK_COLOR_COMPONENT_R_BIT or VK_COLOR_COMPONENT_G_BIT or VK_COLOR_COMPONENT_B_BIT or VK_COLOR_COMPONENT_A_BIT)
            val colorBlend = VkPipelineColorBlendStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO).pAttachments(blendAttach)

            val dynStates = stack.mallocInt(2).put(VK_DYNAMIC_STATE_VIEWPORT).put(VK_DYNAMIC_STATE_SCISSOR).flip()
            val dynamicState = VkPipelineDynamicStateCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO).pDynamicStates(dynStates)

            val pipelineInfo = VkGraphicsPipelineCreateInfo.calloc(1, stack)
            pipelineInfo[0].sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                .pStages(stages).pVertexInputState(vertexInput).pInputAssemblyState(inputAssembly)
                .pViewportState(viewportState).pRasterizationState(raster).pMultisampleState(multisample)
                .pColorBlendState(colorBlend).pDynamicState(dynamicState).layout(layout).renderPass(renderPass).subpass(0)

            val pPipeline = stack.mallocLong(1)
            check(vkCreateGraphicsPipelines(VKUtils.device, VK_NULL_HANDLE, pipelineInfo, null, pPipeline) == VK_SUCCESS)
            return pPipeline[0]
        }
    }

    private fun createShaderModule(spirvBytes: ByteBuffer): Long {
        MemoryStack.stackPush().use { stack ->
            val info = VkShaderModuleCreateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO).pCode(spirvBytes)
            val pModule = stack.mallocLong(1)
            check(vkCreateShaderModule(VKUtils.device, info, null, pModule) == VK_SUCCESS)
            return pModule[0]
        }
    }

    private fun loadSpirv(path: String): ByteBuffer {
        val id = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, path)
        val bytes = net.minecraft.client.Minecraft.getInstance().resourceManager.getResource(id)
            .orElseThrow { RuntimeException("Missing SPIR-V shader: $id") }.open().readBytes()
        return MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
    }

    fun destroy() {
        val d = VKUtils.device
        vkDestroyPipeline(d, shapePipeline, null); vkDestroyPipeline(d, shapePipelineMasked, null); vkDestroyPipeline(d, texturePipeline, null)
        vkDestroyPipelineLayout(d, shapePipelineLayout, null); vkDestroyPipelineLayout(d, texturePipelineLayout, null)
        vkDestroyDescriptorSetLayout(d, textureDescSetLayout, null)
        vkDestroyShaderModule(d, shapeVert, null); vkDestroyShaderModule(d, shapeFrag, null)
        vkDestroyShaderModule(d, texVert, null); vkDestroyShaderModule(d, texFrag, null)
        vkDestroyRenderPass(d, renderPass, null)
    }
}
