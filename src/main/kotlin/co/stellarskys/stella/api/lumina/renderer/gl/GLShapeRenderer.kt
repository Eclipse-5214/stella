package co.stellarskys.stella.api.lumina.renderer.gl

import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.lwjgl.opengl.GL33C
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer
import kotlin.math.*

/*
 * Shape tessellation and AA fringe technique adapted from NanoVG by Mikko Mononen.
 * Porter-Duff SOURCE_IN masking adapted from NVGRenderer.kt in OdinFabric.
 * Built with the assistance of Claude (Anthropic).
 *
 * NanoVG: https://github.com/memononen/nanovg (zlib license)
 * OdinFabric: https://github.com/odtheking/OdinFabric
 * BSD 3-Clause License, Copyright (c) 2025, odtheking
 */
internal object GLShapeRenderer {
    private const val FLOATS = 7
    private const val MAX_VERTS = 65536

    private var program = 0; private var vao = 0; private var vbo = 0
    private var uProjection = -1; private var initialized = false
    private var buf: FloatBuffer = MemoryUtil.memAllocFloat(MAX_VERTS * FLOATS)
    private var vCount = 0

    private data class DrawRun(val scissor: Lumina.ScissorRect?, val start: Int, val count: Int, val stencilOp: Int = 0)
    private val runs = mutableListOf<DrawRun>()

    fun render(shapes: List<Lumina.QueuedShape>, vw: Int, vh: Int) {
        if (shapes.isEmpty()) return
        ensureInit()
        vCount = 0; buf.clear(); runs.clear()

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
                    if (s.scissor != sc) { if (vCount > rs) runs.add(DrawRun(sc, rs, vCount - rs)); sc = s.scissor; rs = vCount }
                    if (s.border > 0f) hollowRect(s) else filledRect(s)
                }
            }
        }
        if (vCount > rs) runs.add(DrawRun(sc, rs, vCount - rs))
        if (vCount == 0) return

        val state = GLUtils.saveGLState()
        GL33C.glUseProgram(program); GL33C.glBindVertexArray(vao)
        GL33C.glEnable(GL33C.GL_BLEND); GL33C.glDisable(GL33C.GL_DEPTH_TEST); GL33C.glDisable(GL33C.GL_CULL_FACE)
        GL33C.glBlendFunc(GL33C.GL_ONE, GL33C.GL_ONE_MINUS_SRC_ALPHA)
        GL33C.glUniformMatrix4fv(uProjection, false, GLUtils.orthoProjection(vw, vh))
        GLUtils.uploadVertices(vbo, buf, vCount, FLOATS)
        for (r in runs) {
            when (r.stencilOp) {
                1 -> {
                    GLUtils.applyScissor(r.scissor, vh)
                    GL33C.glDrawArrays(GL33C.GL_TRIANGLES, r.start, r.count)
                    GL33C.glBlendFunc(GL33C.GL_DST_ALPHA, GL33C.GL_ZERO)
                }
                2 -> GL33C.glBlendFunc(GL33C.GL_ONE, GL33C.GL_ONE_MINUS_SRC_ALPHA)
                else -> {
                    GLUtils.applyScissor(r.scissor, vh)
                    GL33C.glDrawArrays(GL33C.GL_TRIANGLES, r.start, r.count)
                }
            }
        }
        state.restore()
    }

    fun destroy() {
        if (!initialized) return
        GL33C.glDeleteProgram(program); GL33C.glDeleteVertexArrays(vao); GL33C.glDeleteBuffers(vbo)
        MemoryUtil.memFree(buf); program = 0; vao = 0; vbo = 0; initialized = false
    }

    private fun ensureInit() {
        if (initialized) return
        program = GLUtils.linkProgram("shaders/core/lumina.vsh", "shaders/core/lumina.fsh", "LuminaGL")
        uProjection = GL33C.glGetUniformLocation(program, "uProjection")
        vao = GL33C.glGenVertexArrays(); vbo = GL33C.glGenBuffers()
        GL33C.glBindVertexArray(vao)
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo)
        GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, MAX_VERTS.toLong() * FLOATS * 4L, GL33C.GL_DYNAMIC_DRAW)
        val stride = FLOATS * 4
        GL33C.glEnableVertexAttribArray(0); GL33C.glVertexAttribPointer(0, 2, GL33C.GL_FLOAT, false, stride, 0L)
        GL33C.glEnableVertexAttribArray(1); GL33C.glVertexAttribPointer(1, 4, GL33C.GL_FLOAT, false, stride, 8L)
        GL33C.glEnableVertexAttribArray(2); GL33C.glVertexAttribPointer(2, 1, GL33C.GL_FLOAT, false, stride, 24L)
        GL33C.glBindVertexArray(0); GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, 0)
        initialized = true
    }

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
            else -> return GLUtils.unpackPremultiplied(s.color)
        }
        val c1 = GLUtils.unpackPremultiplied(s.gradC1); val c2 = GLUtils.unpackPremultiplied(s.gradC2)
        return floatArrayOf(c1[0]+(c2[0]-c1[0])*t, c1[1]+(c2[1]-c1[1])*t, c1[2]+(c2[2]-c1[2])*t, c1[3]+(c2[3]-c1[3])*t)
    }

    private fun fringeWidth(t: Matrix3x2f): Float {
        val sx = sqrt(t.m00 * t.m00 + t.m01 * t.m01)
        return (1f / Lumina.dpr) / if (sx > 1e-6f) sx else 1f
    }
}
