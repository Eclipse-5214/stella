# Vulkan Backend Implementation Guide for Lumina

## Approach: Raw Vulkan, Shared Device

MC's high-level pipeline API doesn't give you the precision needed for Lumina's fringe-based AA, per-vertex coverage, and mid-frame blend switching. Instead, you'll use **raw LWJGL Vulkan calls** — the same approach as your current raw GL33C code — while **reusing MC 26.2's Vulkan device, queues, and memory allocator** so you don't have to create your own instance.

MC 26.2 exposes everything as public methods on the Vulkan wrapper classes:

| What you need | MC class | Public accessor |
|---|---|---|
| `VkDevice` | `VulkanDevice` | `.vkDevice()` → `org.lwjgl.vulkan.VkDevice` |
| `VkInstance` | `VulkanInstance` | `.vkInstance()` → `org.lwjgl.vulkan.VkInstance` |
| `VkPhysicalDevice` | `VulkanPhysicalDevice` | `.vkPhysicalDevice()` → `org.lwjgl.vulkan.VkPhysicalDevice` |
| `VkQueue` (graphics) | `VulkanQueue` | `.vkQueue()` → `org.lwjgl.vulkan.VkQueue` |
| Queue family index | `VulkanQueue` | `.queueFamilyIndex()` → `int` |
| VMA allocator | `VulkanDevice` | `.vma()` → `long` (VmaAllocator handle) |
| Command buffers | `VulkanCommandPool` | `.allocateBuffer()` → `VkCommandBuffer` |
| Queue submission | `VulkanQueue` | `.beginSubmit()` → `Submission` |
| `VkImage` | `VulkanGpuTexture` | `.vkImage()` → `long` |
| `VkImageView` | `VulkanGpuTextureView` | `.vkImageView()` → `long` |
| `VkBuffer` | `VulkanGpuBuffer` | `.vkBuffer()` → `long` |
| `VkSampler` | `VulkanGpuSampler` | `.vkSampler()` → `long` |

### Getting the VulkanDevice at Runtime

```kotlin
val gpuDevice = RenderSystem.getDevice() as AccessorGpuDevice
val vkDevice = gpuDevice.getBackend() as? VulkanDevice ?: return  // null on GL
val device: VkDevice = vkDevice.vkDevice()
val queue: VkQueue = vkDevice.graphicsQueue().vkQueue()
val queueFamily: Int = vkDevice.graphicsQueue().queueFamilyIndex()
val vma: Long = vkDevice.vma()
val instance: VkInstance = vkDevice.instance().vkInstance()
val physDevice: VkPhysicalDevice = // need accessor (see below)
```

---

## Prerequisites

### 1. Classtweaker Access

Add to `stella.classtweaker`:
```
accessible class com/mojang/blaze3d/vulkan/VulkanDevice
accessible class com/mojang/blaze3d/vulkan/VulkanInstance
accessible class com/mojang/blaze3d/vulkan/VulkanQueue
accessible class com/mojang/blaze3d/vulkan/VulkanPhysicalDevice
accessible class com/mojang/blaze3d/vulkan/VulkanCommandPool
accessible class com/mojang/blaze3d/vulkan/VulkanGpuTexture
accessible class com/mojang/blaze3d/vulkan/VulkanGpuTextureView
accessible class com/mojang/blaze3d/vulkan/VulkanGpuBuffer
accessible class com/mojang/blaze3d/vulkan/VulkanGpuSampler
```

### 2. Accessor Mixin for VulkanDevice Internals

`VulkanDevice` has a public `instance()` method but the `VulkanInstance` doesn't directly expose the `VulkanPhysicalDevice`. You'll likely need an accessor for that if you need physical device properties (memory types, limits). Check if `VulkanDevice` stores it or if you can get it from the instance:

```java
// Only if needed — VulkanDevice may not store physicalDevice directly
// You can query it yourself via vkEnumeratePhysicalDevices on the VkInstance
```

### 3. LWJGL Vulkan Dependency

MC 26.2 bundles LWJGL with Vulkan. Ensure compile-time access:
```kotlin
// build.gradle.kts — only if not already on classpath via Loom
compileOnly("org.lwjgl:lwjgl-vulkan:${lwjglVersion}")
```

Check what LWJGL version MC 26.2 ships and match it.

---

## File Structure

```
lumina/renderer/
├── LuminaBackend.kt             (unchanged)
├── gl/                           (unchanged, used on 26.1 and 26.2-GL)
│   ├── GLBackend.kt
│   ├── GLShapeRenderer.kt
│   ├── GLTextureRenderer.kt
│   └── GLUtils.kt
└── vk/                           ← NEW (raw Vulkan via LWJGL)
    ├── VkLuminaBackend.kt        (implements LuminaBackend)
    ├── VkShapeRenderer.kt        (tessellation + Vulkan draw)
    ├── VkTextureRenderer.kt      (tessellation + Vulkan draw)
    ├── VkPipelineManager.kt      (pipeline creation + caching)
    └── VkUtils.kt                (shared Vulkan helpers)
```

---

## Phase 1: VkUtils.kt — Device Access & Helpers

```kotlin
package co.stellarskys.stella.api.lumina.renderer.vk

import co.stellarskys.stella.mixins.accessors.AccessorGpuDevice
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vulkan.VulkanDevice
import com.mojang.blaze3d.vulkan.VulkanCommandPool
import org.lwjgl.system.MemoryStack
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*

internal object VkUtils {
    // Cached from MC's device — set once during init
    lateinit var device: VkDevice
    lateinit var graphicsQueue: VkQueue
    var queueFamily: Int = -1
    var vma: Long = 0L  // VMA allocator handle
    lateinit var mcVulkanDevice: VulkanDevice

    // Owned by Lumina
    var commandPool: Long = VK_NULL_HANDLE
    var descriptorPool: Long = VK_NULL_HANDLE

    fun init() {
        val gpuDevice = RenderSystem.getDevice() as AccessorGpuDevice
        mcVulkanDevice = gpuDevice.getBackend() as VulkanDevice
        device = mcVulkanDevice.vkDevice()
        graphicsQueue = mcVulkanDevice.graphicsQueue().vkQueue()
        queueFamily = mcVulkanDevice.graphicsQueue().queueFamilyIndex()
        vma = mcVulkanDevice.vma()

        createCommandPool()
        createDescriptorPool()
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
```

---

## Phase 2: SPIR-V Shaders

Since you're bypassing MC's shader system and using raw pipelines, you need SPIR-V. Compile at build time with `glslc`.

### Shape Shader

**`lumina_shape.vert`**
```glsl
#version 450

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec4 aColor;
layout(location = 2) in float aCoverage;

layout(push_constant) uniform PushConstants {
    mat4 uProjection;
};

layout(location = 0) out vec4 vColor;
layout(location = 1) out float vCoverage;

void main() {
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
    vColor = aColor;
    vCoverage = aCoverage;
}
```

**`lumina_shape.frag`**
```glsl
#version 450

layout(location = 0) in vec4 vColor;
layout(location = 1) in float vCoverage;
layout(location = 0) out vec4 fragColor;

void main() {
    fragColor = vColor * vCoverage;
}
```

### Texture Shader

**`lumina_tex.vert`**
```glsl
#version 450

layout(location = 0) in vec2 aPos;
layout(location = 1) in vec2 aUV;
layout(location = 2) in vec4 aColor;

layout(push_constant) uniform PushConstants {
    mat4 uProjection;
    int uMode;
};

layout(location = 0) out vec2 vUV;
layout(location = 1) out vec4 vColor;

void main() {
    gl_Position = uProjection * vec4(aPos, 0.0, 1.0);
    vUV = aUV;
    vColor = aColor;
}
```

**`lumina_tex.frag`**
```glsl
#version 450

layout(set = 0, binding = 0) uniform sampler2D uTex;

layout(push_constant) uniform PushConstants {
    layout(offset = 64) int uMode;
};

layout(location = 0) in vec2 vUV;
layout(location = 1) in vec4 vColor;
layout(location = 0) out vec4 fragColor;

void main() {
    if (uMode == 0) {
        float a = texture(uTex, vUV).r;
        fragColor = vColor * a;
    } else {
        fragColor = texture(uTex, vUV) * vColor;
    }
}
```

### Compiling

```bash
glslc lumina_shape.vert -o lumina_shape_vert.spv
glslc lumina_shape.frag -o lumina_shape_frag.spv
glslc lumina_tex.vert   -o lumina_tex_vert.spv
glslc lumina_tex.frag   -o lumina_tex_frag.spv
```

Place `.spv` files in `src/main/resources/assets/stella/shaders/spirv/`.

You can automate this in Gradle:
```kotlin
tasks.register<Exec>("compileShaders") {
    commandLine("glslc", "src/main/resources/.../lumina_shape.vert", "-o", "src/main/resources/.../lumina_shape_vert.spv")
    // etc.
}
tasks.named("processResources") { dependsOn("compileShaders") }
```

Or use MC's `GlslCompiler` at runtime (it's accessible on VulkanDevice as `glslCompiler` — you'd need an accessor for that private field).

---

## Phase 3: VkPipelineManager.kt — Pipelines & Render Pass

This is where the GL→Vulkan translation is densest. Each pipeline bundles what GL does dynamically: shaders, vertex layout, blend state.

```kotlin
internal object VkPipelineManager {
    // Pipelines
    var shapePipeline: Long = VK_NULL_HANDLE
    var shapePipelineMasked: Long = VK_NULL_HANDLE  // DST_ALPHA blend for drawMasked
    var texturePipeline: Long = VK_NULL_HANDLE

    // Layouts
    var shapePipelineLayout: Long = VK_NULL_HANDLE
    var texturePipelineLayout: Long = VK_NULL_HANDLE
    var textureDescSetLayout: Long = VK_NULL_HANDLE

    // Render pass
    var renderPass: Long = VK_NULL_HANDLE

    // Shader modules
    private var shapeVert: Long = VK_NULL_HANDLE
    private var shapeFrag: Long = VK_NULL_HANDLE
    private var texVert: Long = VK_NULL_HANDLE
    private var texFrag: Long = VK_NULL_HANDLE

    fun init() {
        loadShaderModules()
        createRenderPass()
        createDescriptorSetLayout()
        createShapePipeline()
        createTexturePipeline()
    }

    private fun loadShaderModules() {
        shapeVert = createShaderModule(loadSpirv("shaders/spirv/lumina_shape_vert.spv"))
        shapeFrag = createShaderModule(loadSpirv("shaders/spirv/lumina_shape_frag.spv"))
        texVert = createShaderModule(loadSpirv("shaders/spirv/lumina_tex_vert.spv"))
        texFrag = createShaderModule(loadSpirv("shaders/spirv/lumina_tex_frag.spv"))
    }

    private fun createRenderPass() {
        // Single color attachment: R8G8B8A8_UNORM
        // Load op: LOAD (we're rendering into MC's existing framebuffer)
        // Store op: STORE
        // No depth attachment (2D UI)
        MemoryStack.stackPush().use { stack ->
            val colorAttachment = VkAttachmentDescription.calloc(1, stack)
            colorAttachment[0]
                .format(VK_FORMAT_R8G8B8A8_UNORM)
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
            check(vkCreateRenderPass(VkUtils.device, rpInfo, null, pRenderPass) == VK_SUCCESS)
            renderPass = pRenderPass[0]
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
            check(vkCreateDescriptorSetLayout(VkUtils.device, info, null, pLayout) == VK_SUCCESS)
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
        // VkPipelineLayoutCreateInfo with push constant range (VERTEX|FRAGMENT)
        // and descriptor set layouts
        TODO()
    }

    private fun buildGraphicsPipeline(
        layout: Long, vertModule: Long, fragModule: Long,
        bindingStride: Int, attributes: List<Attr>,
        srcBlend: Int, dstBlend: Int
    ): Long {
        // VkGraphicsPipelineCreateInfo with:
        //   - shader stages (vert + frag)
        //   - vertex input state (binding + attributes)
        //   - input assembly (TRIANGLE_LIST)
        //   - viewport state (dynamic)
        //   - rasterization (fill, no cull, no depth bias)
        //   - multisample (1 sample)
        //   - color blend (srcBlend, dstBlend for both RGB and alpha)
        //   - dynamic state (VIEWPORT, SCISSOR)
        //   - render pass + subpass 0
        TODO()
    }

    private fun createShaderModule(spirvBytes: ByteBuffer): Long {
        MemoryStack.stackPush().use { stack ->
            val info = VkShaderModuleCreateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                .pCode(spirvBytes)
            val pModule = stack.mallocLong(1)
            check(vkCreateShaderModule(VkUtils.device, info, null, pModule) == VK_SUCCESS)
            return pModule[0]
        }
    }

    private fun loadSpirv(path: String): ByteBuffer {
        // Load from resources, return as direct ByteBuffer
        val bytes = VkPipelineManager::class.java.getResourceAsStream("/$path")!!.readBytes()
        val buf = org.lwjgl.system.MemoryUtil.memAlloc(bytes.size)
        buf.put(bytes).flip()
        return buf
    }

    fun destroy() {
        val d = VkUtils.device
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
```

---

## Phase 4: VkShapeRenderer.kt

Tessellation is **identical** to `GLShapeRenderer` — copy `filledRect()`, `hollowRect()`, `emitFringe()`, `computeNormals()`, `emit()`, `tp()`, `colorAt()`, `fringeWidth()` verbatim. They only write to a `FloatBuffer` and have zero GL dependency.

The new part is vertex buffer management and command recording.

```kotlin
internal object VkShapeRenderer {
    private const val FLOATS = 7   // matches GL: x, y, r, g, b, a, coverage
    private const val MAX_VERTS = 65536

    // CPU-side tessellation buffer (same as GL)
    private var buf: FloatBuffer = MemoryUtil.memAllocFloat(MAX_VERTS * FLOATS)
    private var vCount = 0

    // GPU resources
    private var vertexBuffer: Long = VK_NULL_HANDLE
    private var vertexBufferAlloc: Long = 0L
    private var stagingBuffer: Long = VK_NULL_HANDLE
    private var stagingAlloc: Long = 0L
    private var stagingMapped: Long = 0L  // persistently mapped pointer

    private data class DrawRun(val scissor: Lumina.ScissorRect?, val start: Int, val count: Int, val stencilOp: Int = 0)
    private val runs = mutableListOf<DrawRun>()

    fun init() {
        val bufSize = MAX_VERTS.toLong() * FLOATS * 4

        // Staging buffer: HOST_VISIBLE | HOST_COHERENT, persistently mapped
        // Use VMA: vmaCreateBuffer with VMA_ALLOCATION_CREATE_MAPPED_BIT
        // Store stagingBuffer, stagingAlloc, stagingMapped

        // Device-local vertex buffer: VERTEX_BUFFER | TRANSFER_DST
        // Use VMA: vmaCreateBuffer with DEVICE_LOCAL
        // Store vertexBuffer, vertexBufferAlloc
    }

    fun render(cmd: VkCommandBuffer, shapes: List<Lumina.QueuedShape>, vw: Int, vh: Int) {
        if (shapes.isEmpty()) return
        vCount = 0; buf.clear(); runs.clear()

        // === TESSELLATION (copy-paste from GLShapeRenderer) ===
        var sc = shapes.firstOrNull()?.scissor
        var rs = 0
        for (s in shapes) {
            when (s.stencilOp) {
                1 -> {
                    if (vCount > rs) runs.add(DrawRun(sc, rs, vCount - rs))
                    sc = s.scissor; rs = vCount
                    filledRect(s)
                    if (vCount > rs) runs.add(DrawRun(sc, rs, vCount - rs, stencilOp = 1))
                    rs = vCount
                }
                2 -> {
                    if (vCount > rs) runs.add(DrawRun(sc, rs, vCount - rs))
                    runs.add(DrawRun(null, 0, 0, stencilOp = 2))
                    rs = vCount
                }
                else -> {
                    if (s.scissor != sc) {
                        if (vCount > rs) runs.add(DrawRun(sc, rs, vCount - rs))
                        sc = s.scissor; rs = vCount
                    }
                    if (s.border > 0f) hollowRect(s) else filledRect(s)
                }
            }
        }
        if (vCount > rs) runs.add(DrawRun(sc, rs, vCount - rs))
        if (vCount == 0) return

        // === UPLOAD ===
        // memcpy buf → stagingMapped
        buf.position(0).limit(vCount * FLOATS)
        MemoryUtil.memCopy(
            MemoryUtil.memAddress(buf), stagingMapped,
            vCount.toLong() * FLOATS * 4
        )

        // Copy staging → device vertex buffer
        val copyRegion = VkBufferCopy.calloc(1)
        copyRegion[0].srcOffset(0).dstOffset(0).size(vCount.toLong() * FLOATS * 4)
        vkCmdCopyBuffer(cmd, stagingBuffer, vertexBuffer, copyRegion)
        copyRegion.free()

        // Barrier: transfer → vertex input
        val barrier = VkBufferMemoryBarrier.calloc(1)
        barrier[0]
            .sType(VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER)
            .srcAccessMask(VK_ACCESS_TRANSFER_WRITE_BIT)
            .dstAccessMask(VK_ACCESS_VERTEX_ATTRIBUTE_READ_BIT)
            .buffer(vertexBuffer).offset(0).size(VK_WHOLE_SIZE)
        vkCmdPipelineBarrier(cmd,
            VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_VERTEX_INPUT_BIT,
            0, null, barrier, null)
        barrier.free()

        // === DRAW ===
        vkCmdBindVertexBuffers(cmd, 0, longArrayOf(vertexBuffer), longArrayOf(0))

        // Push projection matrix
        val proj = VkUtils.orthoProjection(vw, vh)
        MemoryStack.stackPush().use { stack ->
            val projBuf = stack.mallocFloat(16)
            projBuf.put(proj).flip()
            vkCmdPushConstants(cmd, VkPipelineManager.shapePipelineLayout,
                VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT,
                0, projBuf)
        }

        // Set viewport
        MemoryStack.stackPush().use { stack ->
            val viewport = VkViewport.calloc(1, stack)
            viewport[0].x(0f).y(0f).width(vw.toFloat()).height(vh.toFloat())
                .minDepth(0f).maxDepth(1f)
            vkCmdSetViewport(cmd, 0, viewport)
        }

        var currentPipeline = VkPipelineManager.shapePipeline
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline)

        for (r in runs) {
            when (r.stencilOp) {
                1 -> {
                    // Draw the mask shape normally, then switch to masked blend
                    applyScissor(cmd, r.scissor, vw, vh)
                    vkCmdDraw(cmd, r.count, 1, r.start, 0)
                    // Switch to masked pipeline
                    currentPipeline = VkPipelineManager.shapePipelineMasked
                    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline)
                }
                2 -> {
                    // End masking — switch back to normal pipeline
                    currentPipeline = VkPipelineManager.shapePipeline
                    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline)
                }
                else -> {
                    applyScissor(cmd, r.scissor, vw, vh)
                    vkCmdDraw(cmd, r.count, 1, r.start, 0)
                }
            }
        }
    }

    private fun applyScissor(cmd: VkCommandBuffer, scissor: Lumina.ScissorRect?, vw: Int, vh: Int) {
        MemoryStack.stackPush().use { stack ->
            val rect = VkRect2D.calloc(1, stack)
            if (scissor != null) {
                rect[0].offset().x(scissor.x.toInt()).y(scissor.y.toInt())
                rect[0].extent().width(scissor.w.toInt()).height(scissor.h.toInt())
            } else {
                rect[0].offset().x(0).y(0)
                rect[0].extent().width(vw).height(vh)
            }
            vkCmdSetScissor(cmd, 0, rect)
        }
    }

    fun destroy() {
        // vmaDestroyBuffer for vertexBuffer and stagingBuffer
        MemoryUtil.memFree(buf)
    }

    // === TESSELLATION METHODS — copy from GLShapeRenderer unchanged ===
    // filledRect(s), hollowRect(s), emitFringe(...), computeNormals(...),
    // emit(p, c, cov), tp(x, y, t), tp(p, t), colorAt(s, lx, ly), fringeWidth(t)
    //
    // Only change: GLUtils.unpackPremultiplied → VkUtils.unpackPremultiplied
}
```

---

## Phase 5: VkTextureRenderer.kt

Same pattern. Tessellation from `GLTextureRenderer` (copy `tessText()`, `tessImage()`, `emit()`), new Vulkan submission.

Key difference from shapes: you need **descriptor sets** for texture binding.

```kotlin
internal object VkTextureRenderer {
    private const val FLOATS = 8
    private const val MAX_VERTS = 32768

    // CPU tessellation (same as GL)
    private var buf: FloatBuffer = MemoryUtil.memAllocFloat(MAX_VERTS * FLOATS)
    private var vCount = 0

    // GPU buffers (same pattern as VkShapeRenderer)
    // ...

    // Descriptor set cache: textureId → VkDescriptorSet
    private val descriptorSets = mutableMapOf<Int, Long>()

    fun render(cmd: VkCommandBuffer, text: List<LuminaBackend.TextEntry>,
               images: List<LuminaBackend.ImageEntry>, vw: Int, vh: Int) {
        if (text.isEmpty() && images.isEmpty()) return
        text.forEach { it.font.ensureBaked(); it.font.ensureTextureUploaded() }

        // Tessellate (same as GLTextureRenderer)
        vCount = 0; buf.clear(); runs.clear()
        // ... build DrawRuns batched by (scissor, tex, mode) ...

        if (vCount == 0) return

        // Upload vertices (same pattern as VkShapeRenderer)
        // ...

        // Bind pipeline + push projection
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, VkPipelineManager.texturePipeline)
        vkCmdBindVertexBuffers(cmd, 0, longArrayOf(vertexBuffer), longArrayOf(0))
        // Push projection (first 64 bytes)
        // ...

        var boundTex = -1
        var boundMode = -1

        for (r in runs) {
            // Push mode if changed (offset 64, 4 bytes)
            if (r.mode != boundMode) {
                MemoryStack.stackPush().use { stack ->
                    val modeBuf = stack.mallocInt(1).put(r.mode).flip()
                    vkCmdPushConstants(cmd, VkPipelineManager.texturePipelineLayout,
                        VK_SHADER_STAGE_FRAGMENT_BIT, 64, modeBuf)
                }
                boundMode = r.mode
            }

            // Bind descriptor set for texture if changed
            if (r.tex != boundTex) {
                val descSet = getOrCreateDescriptorSet(r.tex)
                vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS,
                    VkPipelineManager.texturePipelineLayout, 0,
                    longArrayOf(descSet), null)
                boundTex = r.tex
            }

            applyScissor(cmd, r.scissor, vw, vh)
            vkCmdDraw(cmd, r.count, 1, r.start, 0)
        }
    }

    private fun getOrCreateDescriptorSet(textureId: Int): Long {
        return descriptorSets.getOrPut(textureId) {
            val texHandle = VkLuminaBackend.getTextureHandle(textureId)
            allocateAndWriteDescriptorSet(texHandle.imageView, texHandle.sampler)
        }
    }

    private fun allocateAndWriteDescriptorSet(imageView: Long, sampler: Long): Long {
        MemoryStack.stackPush().use { stack ->
            // Allocate from descriptor pool
            val layouts = stack.mallocLong(1).put(VkPipelineManager.textureDescSetLayout).flip()
            val allocInfo = VkDescriptorSetAllocateInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(VkUtils.descriptorPool)
                .pSetLayouts(layouts)
            val pSet = stack.mallocLong(1)
            check(vkAllocateDescriptorSets(VkUtils.device, allocInfo, pSet) == VK_SUCCESS)
            val set = pSet[0]

            // Write the combined image sampler
            val imageInfo = VkDescriptorImageInfo.calloc(1, stack)
            imageInfo[0]
                .sampler(sampler)
                .imageView(imageView)
                .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL)
            val write = VkWriteDescriptorSet.calloc(1, stack)
            write[0]
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(set)
                .dstBinding(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .pImageInfo(imageInfo)
            vkUpdateDescriptorSets(VkUtils.device, write, null)
            return set
        }
    }
}
```

---

## Phase 6: VkLuminaBackend.kt

```kotlin
package co.stellarskys.stella.api.lumina.renderer.vk

object VkLuminaBackend : LuminaBackend {
    private var initialized = false

    // Texture registry: Lumina ID → Vulkan handles
    data class VkTextureHandle(
        val image: Long,
        val allocation: Long,
        val imageView: Long,
        val sampler: Long
    )
    private val textures = mutableMapOf<Int, VkTextureHandle>()
    private var nextTexId = 1

    // Per-frame command buffer
    private var commandBuffer: VkCommandBuffer? = null
    private var fence: Long = VK_NULL_HANDLE

    // Framebuffer for current render target
    private var framebuffer: Long = VK_NULL_HANDLE
    private var fbWidth = 0
    private var fbHeight = 0

    fun ensureInit() {
        if (initialized) return
        VkUtils.init()
        VkPipelineManager.init()
        VkShapeRenderer.init()
        VkTextureRenderer.init()
        createFence()
        commandBuffer = VkUtils.allocateCommandBuffer()
        initialized = true
    }

    override fun renderShapes(shapes: List<Lumina.QueuedShape>, vw: Int, vh: Int) {
        val cmd = commandBuffer ?: return
        VkShapeRenderer.render(cmd, shapes, vw, vh)
    }

    override fun renderTextured(text: List<LuminaBackend.TextEntry>,
                                 images: List<LuminaBackend.ImageEntry>, vw: Int, vh: Int) {
        val cmd = commandBuffer ?: return
        VkTextureRenderer.render(cmd, text, images, vw, vh)
    }

    override fun uploadTexture(width: Int, height: Int, data: ByteBuffer,
                               format: LuminaBackend.TextureFormat, mipmap: Boolean): Int {
        val vkFormat = when (format) {
            LuminaBackend.TextureFormat.RGBA -> VK_FORMAT_R8G8B8A8_UNORM
            LuminaBackend.TextureFormat.R8 -> VK_FORMAT_R8_UNORM
        }
        // 1. Create VkImage via VMA (vmaCreateImage)
        // 2. Create staging buffer, copy data
        // 3. Record layout transition + copy + layout transition on a one-shot cmd
        // 4. Create VkImageView
        // 5. Create VkSampler (linear, clamp, optional mipmap)
        val handle = VkTextureHandle(image, alloc, view, sampler)
        val id = nextTexId++
        textures[id] = handle
        return id
    }

    override fun deleteTexture(id: Int) {
        val h = textures.remove(id) ?: return
        vkDestroySampler(VkUtils.device, h.sampler, null)
        vkDestroyImageView(VkUtils.device, h.imageView, null)
        // vmaDestroyImage(VkUtils.vma, h.image, h.allocation)
    }

    override fun setupRenderTarget(fboId: Int, width: Int, height: Int) {
        ensureInit()
        // fboId comes from the PIP renderer — it's a GL FBO ID
        // For Vulkan, the PIP target is a VulkanGpuTexture.
        // You need to get the VkImage/VkImageView from the PIP color texture.
        //
        // See "Render Target Integration" section below for the full approach.

        fbWidth = width; fbHeight = height

        // Create/recreate framebuffer for this target image
        // ...

        // Begin command buffer
        val cmd = commandBuffer!!
        vkWaitForFences(VkUtils.device, fence, true, Long.MAX_VALUE)
        vkResetFences(VkUtils.device, fence)
        vkResetCommandBuffer(cmd, 0)

        val beginInfo = VkCommandBufferBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
            .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT)
        vkBeginCommandBuffer(cmd, beginInfo)
        beginInfo.free()

        // Begin render pass
        val rpBeginInfo = VkRenderPassBeginInfo.calloc()
            .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
            .renderPass(VkPipelineManager.renderPass)
            .framebuffer(framebuffer)
        rpBeginInfo.renderArea().offset().x(0).y(0)
        rpBeginInfo.renderArea().extent().width(width).height(height)
        // No clear values — we're loading existing content
        vkCmdBeginRenderPass(cmd, rpBeginInfo, VK_SUBPASS_CONTENTS_INLINE)
        rpBeginInfo.free()
    }

    override fun resetAfterRender() {
        val cmd = commandBuffer ?: return
        vkCmdEndRenderPass(cmd)
        vkEndCommandBuffer(cmd)

        // Submit
        MemoryStack.stackPush().use { stack ->
            val submitInfo = VkSubmitInfo.calloc(stack)
                .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                .pCommandBuffers(stack.pointers(cmd))
            vkQueueSubmit(VkUtils.graphicsQueue, submitInfo, fence)
        }
    }

    override fun destroy() {
        if (!initialized) return
        vkDeviceWaitIdle(VkUtils.device)
        textures.keys.toList().forEach { deleteTexture(it) }
        VkTextureRenderer.destroy()
        VkShapeRenderer.destroy()
        VkPipelineManager.destroy()
        vkDestroyFence(VkUtils.device, fence, null)
        VkUtils.destroy()
        initialized = false
    }

    fun getTextureHandle(id: Int): VkTextureHandle = textures[id]!!
}
```

---

## Phase 7: Render Target Integration (PIP)

This is the trickiest part. The PIP renderer gives you MC's render target, which in Vulkan mode is a `VulkanGpuTexture`. You need its `VkImage` to create a framebuffer.

### In LuminaPIPRenderer (26.2 path):

```kotlin
//? if >= 26.2 {
override fun renderToTexture(state: LuminaRenderState, poseStack: PoseStack,
                              submitNodeCollector: SubmitNodeCollector) {
    val colorTex = RenderSystem.outputColorTextureOverride ?: return
    val (width, height) = colorTex.let { it.getWidth(0) to it.getHeight(0) }

    val gpuDevice = RenderSystem.getDevice() as AccessorGpuDevice
    val backend = gpuDevice.getBackend()

    if (backend is VulkanDevice) {
        // Vulkan path: get raw VkImage from the PIP target texture
        val vkTex = colorTex.texture() as VulkanGpuTexture
        val vkImage = vkTex.vkImage()
        // Pass to VkLuminaBackend to create framebuffer + render
        VkLuminaBackend.setupVulkanRenderTarget(vkImage, width, height)
        state.renderContent(width, height)
        VkLuminaBackend.resetAfterRender()
    } else {
        // GL path: same as before
        val glDevice = backend as GlDevice
        val glColorTex = colorTex.texture() as GlTexture
        val glDepthTex = (RenderSystem.outputDepthTextureOverride?.texture() as? GlTexture) ?: return
        val fboId = glDevice.frameBufferCache().getFbo(
            glDevice.directStateAccess(), listOf(glColorTex), glDepthTex)
        Lumina.backend.setupRenderTarget(fboId, width, height)
        state.renderContent(width, height)
        Lumina.backend.resetAfterRender()
    }
}
//? }
```

### Creating a VkFramebuffer from PIP's VkImage

You need a `VkImageView` for the PIP target image. You can either:
1. Create your own `VkImageView` wrapping the PIP's `VkImage`
2. Use MC's `VulkanGpuTextureView.vkImageView()` if the PIP provides a view

Then create a `VkFramebuffer` pointing that view at your render pass.

---

## Phase 8: Backend Selection

```kotlin
// Lumina.kt
object Lumina {
    internal val backend: LuminaBackend = selectBackend()

    private fun selectBackend(): LuminaBackend {
        //? if >= 26.2 {
        /*
        val gpuDevice = RenderSystem.getDevice() as? AccessorGpuDevice
        val isVulkan = gpuDevice?.getBackend() is VulkanDevice
        return if (isVulkan) VkLuminaBackend else GLBackend
        */
        //? } else {
        return GLBackend
        //? }
    }
}
```

**Note:** `RenderSystem.getDevice()` must be called after MC initializes the rendering backend. If `Lumina` is initialized too early, defer the backend selection to first use.

---

## Synchronization Model

### Per-Frame Flow

```
setupRenderTarget()
  ├─ Wait for previous frame's fence
  ├─ Reset fence + command buffer
  ├─ Begin command buffer
  └─ Begin render pass

renderShapes()  ←─┐
renderTextured() ──┤  Lumina.flush() may call these multiple times
                   │  per frame (shape groups interleaved with texture groups)
                   └─ All recorded into same command buffer

resetAfterRender()
  ├─ End render pass
  ├─ End command buffer
  └─ Submit to graphics queue with fence
```

### Double Buffering (Recommended Optimization)

Use 2 command buffers + 2 fences, alternating each frame:
```kotlin
private val commandBuffers = arrayOfNulls<VkCommandBuffer>(2)
private val fences = LongArray(2) { VK_NULL_HANDLE }
private var currentFrame = 0
```
This way you never stall on the previous frame finishing — you wait on the frame before that.

---

## Pitfalls

1. **Scissor coords** — GL scissor has Y=0 at bottom, Vulkan has Y=0 at top. Your GL code does `viewportHeight - scissor.y - scissor.h`. In Vulkan, use `scissor.y` directly (matches your 2D coordinate system).

2. **Push constant alignment** — The `int uMode` at offset 64 in the texture push constants needs to be aligned to 4 bytes (which it is, since 64 is 4-aligned). But if you add more uniforms, watch the alignment.

3. **Image layout transitions** — When you get the PIP target image, it may be in `COLOR_ATTACHMENT_OPTIMAL` or `UNDEFINED`. Your render pass's `initialLayout` must match (or use `UNDEFINED` to discard existing contents, but you want LOAD not CLEAR for the PIP target).

4. **Queue sharing** — You're using MC's graphics queue. MC also submits work on this queue. Ensure your submissions don't interleave — the PIP callback should be called at a safe point in MC's frame where the queue is idle.

5. **VMA** — MC uses Vulkan Memory Allocator (VMA). Use it for your buffers/images instead of raw `vkAllocateMemory`. The `vma()` handle from `VulkanDevice` gives you access. Import LWJGL's VMA bindings.

6. **Descriptor pool exhaustion** — 128 sets should be plenty for Lumina's use case (one per unique texture per frame). If you hot-swap many textures, grow the pool or free unused sets.

7. **Thread safety** — All Vulkan calls must happen on the render thread. Since `Lumina.flush()` runs on the render thread, this should be fine.

---

## What You Can Reuse from MC (via Shared Device)

| Resource | Create yourself? | Or reuse from MC? |
|---|---|---|
| VkInstance | **Reuse** | `VulkanDevice.instance().vkInstance()` |
| VkDevice | **Reuse** | `VulkanDevice.vkDevice()` |
| VkQueue | **Reuse** | `VulkanDevice.graphicsQueue().vkQueue()` |
| VMA | **Reuse** | `VulkanDevice.vma()` |
| Command pool | **Create your own** | Separate pool for Lumina's command buffers |
| Descriptor pool | **Create your own** | Lumina-specific pool |
| Pipelines | **Create your own** | Lumina's custom shaders/blend/vertex layout |
| Render pass | **Create your own** | Lumina's specific attachment config |
| Vertex buffers | **Create your own** | Lumina's vertex data |
| Textures | **Create your own** | Font atlas, images, SVGs |

---

## Estimated Effort

| Component | Lines | Difficulty |
|---|---|---|
| VkUtils (device access, pool creation) | 80-120 | Medium |
| VkPipelineManager (render pass, pipelines) | 250-350 | High (most Vulkan boilerplate) |
| VkShapeRenderer | 180-220 | Medium (120 copied tessellation + 80 new) |
| VkTextureRenderer | 160-200 | Medium (100 copied tessellation + 80 new) |
| VkLuminaBackend | 100-150 | Medium |
| Texture upload/management | 80-120 | Medium |
| PIP integration | 30-50 | Medium |
| Classtweaker + accessors | 15-25 | Low |
| SPIR-V shaders | 4 files | Low |
| **Total** | **~900-1250** | |

### Implementation Order

1. Classtweaker entries + verify you can access `VulkanDevice.vkDevice()` at runtime
2. VkUtils — get device handles, create command/descriptor pools
3. SPIR-V shaders — compile and load
4. VkPipelineManager — render pass + shape pipeline only
5. VkShapeRenderer — get colored rectangles rendering
6. PIP integration — wire up the render target
7. VkTextureRenderer — descriptor sets for texture binding
8. Texture upload lifecycle
9. Masking pipeline variant
10. Double-buffer optimization
