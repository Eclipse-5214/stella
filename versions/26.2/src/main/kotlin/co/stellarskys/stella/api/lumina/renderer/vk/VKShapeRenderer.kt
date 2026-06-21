package co.stellarskys.stella.api.lumina.renderer.vk

import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil
import org.lwjgl.vulkan.*
import org.lwjgl.vulkan.VK13.*
import java.nio.FloatBuffer
import kotlin.math.*

internal object VKShapeRenderer {
    private const val FLOATS = 7   // matches GL: x, y, r, g, b, a, coverage
    private const val MAX_VERTS = 65536

    // CPU-side tessellation buffer (same as GL)
    private var buf: FloatBuffer = MemoryUtil.memAllocFloat(MAX_VERTS * FLOATS)
    private var vCount = 0

    private var vertexBuf: VKUtils.VmaBuffer? = null

    private data class DrawRun(val scissor: Lumina.ScissorRect?, val start: Int, val count: Int, val stencilOp: Int = 0)
    private val runs = mutableListOf<DrawRun>()

    fun init() {
        val bufSize = MAX_VERTS.toLong() * FLOATS * 4
        vertexBuf = VKUtils.createHostVertexBuffer(bufSize)
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

        // === UPLOAD (direct to host-visible vertex buffer) ===
        val vb = vertexBuf!!
        buf.position(0).limit(vCount * FLOATS)
        MemoryUtil.memCopy(MemoryUtil.memAddress(buf), vb.mappedPtr, vCount.toLong() * FLOATS * 4)

        // === BEGIN RENDER PASS + DRAW ===
        VKBackend.beginRenderPassIfNeeded()
        vkCmdBindVertexBuffers(cmd, 0, longArrayOf(vb.buffer), longArrayOf(0))

        // Push projection matrix
        val proj = VKUtils.orthoProjection(vw, vh)
        MemoryStack.stackPush().use { stack ->
            val projBuf = stack.mallocFloat(16)
            projBuf.put(proj).flip()
            vkCmdPushConstants(cmd, VKPipelineManager.shapePipelineLayout,
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

        var currentPipeline = VKPipelineManager.shapePipeline
        vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline)

        for (r in runs) {
            when (r.stencilOp) {
                1 -> {
                    // Draw the mask shape normally, then switch to masked blend
                    applyScissor(cmd, r.scissor, vw, vh)
                    vkCmdDraw(cmd, r.count, 1, r.start, 0)
                    // Switch to masked pipeline
                    currentPipeline = VKPipelineManager.shapePipelineMasked
                    vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, currentPipeline)
                }
                2 -> {
                    // End masking — switch back to normal pipeline
                    currentPipeline = VKPipelineManager.shapePipeline
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
                val sx = maxOf(0, scissor.x.toInt())
                val sy = maxOf(0, scissor.y.toInt())
                rect[0].offset().x(sx).y(sy)
                rect[0].extent().width(maxOf(0, scissor.w.toInt())).height(maxOf(0, scissor.h.toInt()))
            } else {
                rect[0].offset().x(0).y(0)
                rect[0].extent().width(vw).height(vh)
            }
            vkCmdSetScissor(cmd, 0, rect)
        }
    }

    fun destroy() {
        vertexBuf?.let { VKUtils.destroyBuffer(it) }; vertexBuf = null
        MemoryUtil.memFree(buf)
    }

    // === TESSELLATION METHODS — copy from GLShapeRenderer unchanged ===
    // filledRect(s), hollowRect(s), emitFringe(...), computeNormals(...),
    // emit(p, c, cov), tp(x, y, t), tp(p, t), colorAt(s, lx, ly), fringeWidth(t)
    //
    // Only change: GLUtils.unpackPremultiplied → VKUtils.unpackPremultiplied
    
    private fun filledRect(s: Lumina.QueuedShape) {
        val fw = fringeWidth(s.transform); val hf = fw * 0.5f
        val outline = LuminaBackend.generateOutline(s.x + hf, s.y + hf, max(0f, s.w - fw), max(0f, s.h - fw),
            max(0f, s.tl - hf), max(0f, s.tr - hf), max(0f, s.br - hf), max(0f, s.bl - hf))
        val n = outline.size; if (n < 3) return
        val cx = s.x + s.w * 0.5f; val cy = s.y + s.h * 0.5f
        val cc = colorAt(s, cx, cy); val cp = tp(cx, cy, s.transform)
        for (i in 0 until n) {
            val j = (i + 1) % n
            emit(cp, cc, 1f)
            emit(tp(outline[i], s.transform), colorAt(s, outline[i].x, outline[i].y), 1f)
            emit(tp(outline[j], s.transform), colorAt(s, outline[j].x, outline[j].y), 1f)
        }
        emitFringe(outline, computeNormals(outline), s, fw, 1f)
    }

    private fun hollowRect(s: Lumina.QueuedShape) {
        val b = s.border; val fringe = fringeWidth(s.transform)
        val half = max(0f, b * 0.5f - fringe)
        val outer = LuminaBackend.generateOutline(s.x - half, s.y - half, s.w + half * 2, s.h + half * 2,
            s.tl + half, s.tr + half, s.br + half, s.bl + half)
        val inner = LuminaBackend.generateOutline(s.x + half, s.y + half, s.w - half * 2, s.h - half * 2,
            max(0f, s.tl - half), max(0f, s.tr - half), max(0f, s.br - half), max(0f, s.bl - half))
        val n = outer.size; if (n < 3 || inner.size != n) return
        for (i in 0 until n) {
            val j = (i + 1) % n
            val toi = tp(outer[i], s.transform); val toj = tp(outer[j], s.transform)
            val tii = tp(inner[i], s.transform); val tij = tp(inner[j], s.transform)
            val coi = colorAt(s, outer[i].x, outer[i].y); val coj = colorAt(s, outer[j].x, outer[j].y)
            val cii = colorAt(s, inner[i].x, inner[i].y); val cij = colorAt(s, inner[j].x, inner[j].y)
            emit(toi, coi, 1f); emit(tii, cii, 1f); emit(toj, coj, 1f)
            emit(toj, coj, 1f); emit(tii, cii, 1f); emit(tij, cij, 1f)
        }
        emitFringe(outer, computeNormals(outer), s, fringe, 1f)
        emitFringe(inner, computeNormals(inner), s, fringe, -1f)
    }

    private fun emitFringe(outline: List<Vector2f>, normals: List<Vector2f>, s: Lumina.QueuedShape, fringe: Float, sign: Float) {
        for (i in 0 until outline.size) {
            val j = (i + 1) % outline.size
            val pi = outline[i]; val pj = outline[j]
            val ni = normals[i]; val nj = normals[j]
            val ci = colorAt(s, pi.x, pi.y); val cj = colorAt(s, pj.x, pj.y)
            val ti = tp(pi, s.transform); val tj = tp(pj, s.transform)
            val tio = tp(pi.x + ni.x * fringe * sign, pi.y + ni.y * fringe * sign, s.transform)
            val tjo = tp(pj.x + nj.x * fringe * sign, pj.y + nj.y * fringe * sign, s.transform)
            emit(ti, ci, 1f); emit(tio, ci, 0f); emit(tj, cj, 1f)
            emit(tj, cj, 1f); emit(tio, ci, 0f); emit(tjo, cj, 0f)
        }
    }

    private fun computeNormals(outline: List<Vector2f>) = List(outline.size) { i ->
        val n = outline.size
        val prev = outline[(i - 1 + n) % n]; val curr = outline[i]; val next = outline[(i + 1) % n]
        val dx0 = curr.x - prev.x; val dy0 = curr.y - prev.y; val l0 = sqrt(dx0 * dx0 + dy0 * dy0)
        val dx1 = next.x - curr.x; val dy1 = next.y - curr.y; val l1 = sqrt(dx1 * dx1 + dy1 * dy1)
        var nx = (if (l0 > 1e-6f) dy0 / l0 else 0f) + (if (l1 > 1e-6f) dy1 / l1 else 0f)
        var ny = (if (l0 > 1e-6f) -dx0 / l0 else 0f) + (if (l1 > 1e-6f) -dx1 / l1 else 0f)
        val len = sqrt(nx * nx + ny * ny)
        if (len > 1e-6f) { nx /= len; ny /= len }
        Vector2f(nx, ny)
    }

    private fun emit(p: Vector2f, c: FloatArray, cov: Float) {
        if (vCount >= MAX_VERTS) return
        buf.put(p.x).put(p.y).put(c[0]).put(c[1]).put(c[2]).put(c[3]).put(cov); vCount++
    }
    
    private fun tp(x: Float, y: Float, t: Matrix3x2f) = t.transformPosition(Vector2f(x, y))
    private fun tp(p: Vector2f, t: Matrix3x2f) = t.transformPosition(Vector2f(p))

    private fun colorAt(s: Lumina.QueuedShape, lx: Float, ly: Float): FloatArray {
        val t = when (s.gradType) {
            1 -> if (s.w > 0f) ((lx - s.x) / s.w).coerceIn(0f, 1f) else 0f
            2 -> if (s.h > 0f) ((ly - s.y) / s.h).coerceIn(0f, 1f) else 0f
            3 -> {
                val tx = if (s.w > 0f) (lx - s.x) / s.w else 0f
                val ty = if (s.h > 0f) (ly - s.y) / s.h else 0f
                ((tx + ty) * 0.5f).coerceIn(0f, 1f)
            }
            else -> return VKUtils.unpackPremultiplied(s.color)
        }
        val c1 = VKUtils.unpackPremultiplied(s.gradC1); val c2 = VKUtils.unpackPremultiplied(s.gradC2)
        return floatArrayOf(c1[0]+(c2[0]-c1[0])*t, c1[1]+(c2[1]-c1[1])*t, c1[2]+(c2[2]-c1[2])*t, c1[3]+(c2[3]-c1[3])*t)
    }

    private fun fringeWidth(t: Matrix3x2f): Float {
        val sx = sqrt(t.m00 * t.m00 + t.m01 * t.m01)
        return (1f / Lumina.dpr) / if (sx > 1e-6f) sx else 1f
    }
}