package co.stellarskys.stella.api.lumina.renderer.vk

import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import co.stellarskys.stella.api.lumina.types.LuminaFont
import org.joml.Vector2f
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import java.nio.FloatBuffer

internal object VKTextureRenderer {
    private const val FLOATS = 8
    private const val MAX_VERTS = 32768
    private val tmpQuad = STBTTAlignedQuad.malloc()
    private val tmpX = floatArrayOf(0f)
    private val tmpY = floatArrayOf(0f)
    private var buf: FloatBuffer = MemoryUtil.memAllocFloat(MAX_VERTS * FLOATS)
    private var vCount = 0
    private var bufferOffset = 0
    private var vertexBuf: VKUtils.VmaBuffer? = null
    private data class DrawRun(val scissor: Lumina.ScissorRect?, val tex: Int, val mode: Int, val start: Int, val count: Int)
    private val runs = mutableListOf<DrawRun>()
    private val descriptorSets = mutableMapOf<Int, Long>()

    fun init() { vertexBuf = VKUtils.createHostVertexBuffer(MAX_VERTS.toLong() * FLOATS * 4) }
    fun resetFrame() { bufferOffset = 0 }

    fun render(cmd: VkCommandBuffer, text: List<LuminaBackend.TextEntry>, images: List<LuminaBackend.ImageEntry>, vw: Int, vh: Int) {
        if (text.isEmpty() && images.isEmpty()) return
        text.forEach { it.font.ensureBaked(); it.font.ensureTextureUploaded() }
        vCount = 0; buf.clear(); runs.clear()

        if (text.isNotEmpty()) {
            var sc = text[0].scissor; var tex = text[0].font.atlasTexture; var rs = 0
            for (s in text) {
                val t = s.font.atlasTexture
                if (s.scissor != sc || t != tex) { if (vCount > rs) runs.add(DrawRun(sc, tex, 0, rs, vCount - rs)); sc = s.scissor; tex = t; rs = vCount }
                tessText(s)
            }
            if (vCount > rs) runs.add(DrawRun(sc, tex, 0, rs, vCount - rs))
        }
        for (s in images) { val rs = vCount; tessImage(s); if (vCount > rs) runs.add(DrawRun(s.scissor, s.textureId, 1, rs, vCount - rs)) }
        if (vCount == 0) return

        val vb = vertexBuf!!; val byteOffset = bufferOffset.toLong() * FLOATS * 4
        buf.position(0).limit(vCount * FLOATS)
        MemoryUtil.memCopy(MemoryUtil.memAddress(buf), vb.mappedPtr + byteOffset, vCount.toLong() * FLOATS * 4)

        VKBackend.beginRenderPassIfNeeded()
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, VKPipelineManager.texturePipeline)
        vkCmdBindVertexBuffers(cmd, 0, longArrayOf(vb.buffer), longArrayOf(byteOffset))
        MemoryStack.stackPush().use { stack ->
            val projBuf = stack.mallocFloat(16); projBuf.put(VKUtils.orthoProjection(vw, vh)).flip()
            vkCmdPushConstants(cmd, VKPipelineManager.texturePipelineLayout, VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT, 0, projBuf)
            val viewport = VkViewport.calloc(1, stack)
            viewport[0].x(0f).y(0f).width(vw.toFloat()).height(vh.toFloat()).minDepth(0f).maxDepth(1f)
            vkCmdSetViewport(cmd, 0, viewport)
        }

        var boundTex = -1; var boundMode = -1
        for (r in runs) {
            if (r.mode != boundMode) {
                MemoryStack.stackPush().use { stack ->
                    vkCmdPushConstants(cmd, VKPipelineManager.texturePipelineLayout,
                        VK_SHADER_STAGE_VERTEX_BIT or VK_SHADER_STAGE_FRAGMENT_BIT, 64, stack.mallocInt(1).put(r.mode).flip())
                }
                boundMode = r.mode
            }
            if (r.tex != boundTex) {
                vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, VKPipelineManager.texturePipelineLayout, 0, longArrayOf(getOrCreateDescriptorSet(r.tex)), null)
                boundTex = r.tex
            }
            applyScissor(cmd, r.scissor, vw, vh)
            vkCmdDraw(cmd, r.count, 1, r.start, 0)
        }
        bufferOffset += vCount
    }

    private fun getOrCreateDescriptorSet(textureId: Int) = descriptorSets.getOrPut(textureId) {
        val h = VKBackend.getTextureHandle(textureId)
        MemoryStack.stackPush().use { stack ->
            val layouts = stack.mallocLong(1).put(VKPipelineManager.textureDescSetLayout).flip()
            val allocInfo = VkDescriptorSetAllocateInfo.calloc(stack).sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                .descriptorPool(VKUtils.descriptorPool).pSetLayouts(layouts)
            val pSet = stack.mallocLong(1)
            check(vkAllocateDescriptorSets(VKUtils.device, allocInfo, pSet) == VK_SUCCESS)
            val set = pSet[0]
            val imgInfo = VkDescriptorImageInfo.calloc(1, stack)
            imgInfo[0].sampler(h.sampler).imageView(h.imageView).imageLayout(VK_IMAGE_LAYOUT_GENERAL)
            val write = VkWriteDescriptorSet.calloc(1, stack)
            write[0].sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET).dstSet(set).dstBinding(0)
                .descriptorCount(1).descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER).pImageInfo(imgInfo)
            vkUpdateDescriptorSets(VKUtils.device, write, null)
            set
        }
    }

    private fun applyScissor(cmd: VkCommandBuffer, scissor: Lumina.ScissorRect?, vw: Int, vh: Int) {
        MemoryStack.stackPush().use { stack ->
            val rect = VkRect2D.calloc(1, stack)
            if (scissor != null) {
                rect[0].offset().x(maxOf(0, scissor.x.toInt())).y(maxOf(0, vh - scissor.y.toInt() - scissor.h.toInt()))
                rect[0].extent().width(maxOf(0, scissor.w.toInt())).height(maxOf(0, scissor.h.toInt()))
            } else { rect[0].offset().x(0).y(0); rect[0].extent().width(vw).height(vh) }
            vkCmdSetScissor(cmd, 0, rect)
        }
    }

    private fun tessText(s: LuminaBackend.TextEntry) {
        val scale = s.size / LuminaFont.BAKE_SIZE; val color = VKUtils.unpackPremultiplied(s.color); val asc = s.font.ascentPx
        tmpX[0] = 0f; tmpY[0] = 0f
        for (c in s.text) {
            val ci = c.code - LuminaFont.FIRST_CHAR; if (ci < 0 || ci >= LuminaFont.NUM_CHARS) continue
            s.font.getPackedQuad(ci, tmpX, tmpY, tmpQuad)
            val lx0 = s.x + tmpQuad.x0() * scale; val lx1 = s.x + tmpQuad.x1() * scale
            val ly0 = s.y + (asc + tmpQuad.y0()) * scale + 0.5f; val ly1 = s.y + (asc + tmpQuad.y1()) * scale + 0.5f
            val p00 = s.transform.transformPosition(Vector2f(lx0, ly0)); val p10 = s.transform.transformPosition(Vector2f(lx1, ly0))
            val p01 = s.transform.transformPosition(Vector2f(lx0, ly1)); val p11 = s.transform.transformPosition(Vector2f(lx1, ly1))
            val u0 = tmpQuad.s0(); val v0 = tmpQuad.t0(); val u1 = tmpQuad.s1(); val v1 = tmpQuad.t1()
            emit(p00.x, p00.y, u0, v0, color); emit(p10.x, p10.y, u1, v0, color); emit(p11.x, p11.y, u1, v1, color)
            emit(p00.x, p00.y, u0, v0, color); emit(p11.x, p11.y, u1, v1, color); emit(p01.x, p01.y, u0, v1, color)
        }
    }

    private fun tessImage(s: LuminaBackend.ImageEntry) {
        val color = VKUtils.unpackPremultiplied(s.color)
        fun tp(x: Float, y: Float) = s.transform.transformPosition(Vector2f(x, y))
        if (s.radius < 0.5f) {
            val p00 = tp(s.x, s.y); val p10 = tp(s.x + s.w, s.y); val p01 = tp(s.x, s.y + s.h); val p11 = tp(s.x + s.w, s.y + s.h)
            emit(p00.x, p00.y, s.u0, s.v0, color); emit(p10.x, p10.y, s.u1, s.v0, color); emit(p11.x, p11.y, s.u1, s.v1, color)
            emit(p00.x, p00.y, s.u0, s.v0, color); emit(p11.x, p11.y, s.u1, s.v1, color); emit(p01.x, p01.y, s.u0, s.v1, color)
            return
        }
        val outline = LuminaBackend.generateOutline(s.x, s.y, s.w, s.h, s.radius, s.radius, s.radius, s.radius)
        val n = outline.size; if (n < 3) return
        val cx = s.x + s.w * 0.5f; val cy = s.y + s.h * 0.5f; val cp = tp(cx, cy)
        val cu = s.u0 + (s.u1 - s.u0) * 0.5f; val cv = s.v0 + (s.v1 - s.v0) * 0.5f
        for (i in 0 until n) {
            val j = (i + 1) % n; val pi = outline[i]; val pj = outline[j]
            val ti = tp(pi.x, pi.y); val tj = tp(pj.x, pj.y)
            val ui = s.u0 + (pi.x - s.x) / s.w * (s.u1 - s.u0); val vi = s.v0 + (pi.y - s.y) / s.h * (s.v1 - s.v0)
            val uj = s.u0 + (pj.x - s.x) / s.w * (s.u1 - s.u0); val vj = s.v0 + (pj.y - s.y) / s.h * (s.v1 - s.v0)
            emit(cp.x, cp.y, cu, cv, color); emit(ti.x, ti.y, ui, vi, color); emit(tj.x, tj.y, uj, vj, color)
        }
    }

    private fun emit(x: Float, y: Float, u: Float, v: Float, c: FloatArray) { if (vCount >= MAX_VERTS) return; buf.put(x).put(y).put(u).put(v).put(c[0]).put(c[1]).put(c[2]).put(c[3]); vCount++ }

    fun destroy() { vertexBuf?.let { VKUtils.destroyBuffer(it) }; vertexBuf = null; MemoryUtil.memFree(buf); tmpQuad.free() }
}
