package co.stellarskys.stella.api.lumina.renderer.gl

import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import co.stellarskys.stella.api.lumina.types.LuminaFont
import org.joml.Vector2f
import org.lwjgl.opengl.GL33C
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer

/*
 * Font atlas baking approach adapted from fontstash in NanoVG by Mikko Mononen.
 * Built with the assistance of Claude (Anthropic).
 *
 * NanoVG: https://github.com/memononen/nanovg (zlib license)
 */
internal object GLTextureRenderer {
    private const val FLOATS = 8
    private const val MAX_VERTS = 32768

    private val tmpQuad = STBTTAlignedQuad.malloc()
    private val tmpX = floatArrayOf(0f)
    private val tmpY = floatArrayOf(0f)

    private var program = 0; private var vao = 0; private var vbo = 0
    private var uProjection = -1; private var uTexture = -1; private var uMode = -1
    private var initialized = false

    private var buf: FloatBuffer = MemoryUtil.memAllocFloat(MAX_VERTS * FLOATS)
    private var vCount = 0

    private data class DrawRun(val scissor: Lumina.ScissorRect?, val tex: Int, val mode: Int, val start: Int, val count: Int)
    private val runs = mutableListOf<DrawRun>()

    fun render(text: List<LuminaBackend.TextEntry>, images: List<LuminaBackend.ImageEntry>, vw: Int, vh: Int) {
        if (text.isEmpty() && images.isEmpty()) return
        ensureInit()
        text.forEach { it.font.ensureBaked(); it.font.ensureTextureUploaded() }
        vCount = 0; buf.clear(); runs.clear()

        if (text.isNotEmpty()) {
            var sc = text[0].scissor; var tex = text[0].font.atlasTexture; var rs = 0
            for (s in text) {
                val t = s.font.atlasTexture
                if (s.scissor != sc || t != tex) {
                    if (vCount > rs) runs.add(DrawRun(sc, tex, 0, rs, vCount - rs))
                    sc = s.scissor; tex = t; rs = vCount
                }
                tessText(s)
            }
            if (vCount > rs) runs.add(DrawRun(sc, tex, 0, rs, vCount - rs))
        }

        for (s in images) {
            val rs = vCount; tessImage(s)
            if (vCount > rs) runs.add(DrawRun(s.scissor, s.textureId, 1, rs, vCount - rs))
        }
        if (vCount == 0) return

        val state = GLUtils.saveGLState()
        GL33C.glUseProgram(program); GL33C.glBindVertexArray(vao)
        GL33C.glEnable(GL33C.GL_BLEND); GL33C.glDisable(GL33C.GL_DEPTH_TEST); GL33C.glDisable(GL33C.GL_CULL_FACE)
        GL33C.glDisable(GL33C.GL_STENCIL_TEST)
        GL33C.glColorMask(true, true, true, true)
        GL33C.glBlendFunc(GL33C.GL_ONE, GL33C.GL_ONE_MINUS_SRC_ALPHA)
        GL33C.glActiveTexture(GL33C.GL_TEXTURE0); GL33C.glUniform1i(uTexture, 0)
        GL33C.glUniformMatrix4fv(uProjection, false, GLUtils.orthoProjection(vw, vh))
        GLUtils.uploadVertices(vbo, buf, vCount, FLOATS)

        var boundTex = -1; var boundMode = -1
        for (r in runs) {
            if (r.mode != boundMode) { GL33C.glUniform1i(uMode, r.mode); boundMode = r.mode }
            if (r.tex != boundTex) { GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, r.tex); boundTex = r.tex }
            GLUtils.applyScissor(r.scissor, vh)
            GL33C.glDrawArrays(GL33C.GL_TRIANGLES, r.start, r.count)
        }
        state.restore()
    }

    private fun tessText(s: LuminaBackend.TextEntry) {
        val scale = s.size / LuminaFont.BAKE_SIZE
        val color = GLUtils.unpackPremultiplied(s.color)
        val asc = s.font.ascentPx
        tmpX[0] = 0f; tmpY[0] = 0f
        for (c in s.text) {
            val ci = c.code - LuminaFont.FIRST_CHAR
            if (ci < 0 || ci >= LuminaFont.NUM_CHARS) continue
            s.font.getPackedQuad(ci, tmpX, tmpY, tmpQuad)
            val lx0 = s.x + tmpQuad.x0() * scale; val lx1 = s.x + tmpQuad.x1() * scale
            val ly0 = s.y + (asc + tmpQuad.y0()) * scale + 0.5f
            val ly1 = s.y + (asc + tmpQuad.y1()) * scale + 0.5f
            val p00 = s.transform.transformPosition(Vector2f(lx0, ly0))
            val p10 = s.transform.transformPosition(Vector2f(lx1, ly0))
            val p01 = s.transform.transformPosition(Vector2f(lx0, ly1))
            val p11 = s.transform.transformPosition(Vector2f(lx1, ly1))
            val u0 = tmpQuad.s0(); val v0 = tmpQuad.t0(); val u1 = tmpQuad.s1(); val v1 = tmpQuad.t1()
            emit(p00.x, p00.y, u0, v0, color); emit(p10.x, p10.y, u1, v0, color); emit(p11.x, p11.y, u1, v1, color)
            emit(p00.x, p00.y, u0, v0, color); emit(p11.x, p11.y, u1, v1, color); emit(p01.x, p01.y, u0, v1, color)
        }
    }

    private fun tessImage(s: LuminaBackend.ImageEntry) {
        val color = GLUtils.unpackPremultiplied(s.color)
        fun tp(x: Float, y: Float) = s.transform.transformPosition(Vector2f(x, y))

        if (s.radius < 0.5f) {
            val p00 = tp(s.x, s.y); val p10 = tp(s.x + s.w, s.y)
            val p01 = tp(s.x, s.y + s.h); val p11 = tp(s.x + s.w, s.y + s.h)
            emit(p00.x, p00.y, s.u0, s.v0, color); emit(p10.x, p10.y, s.u1, s.v0, color); emit(p11.x, p11.y, s.u1, s.v1, color)
            emit(p00.x, p00.y, s.u0, s.v0, color); emit(p11.x, p11.y, s.u1, s.v1, color); emit(p01.x, p01.y, s.u0, s.v1, color)
            return
        }

        val outline = LuminaBackend.generateOutline(s.x, s.y, s.w, s.h, s.radius, s.radius, s.radius, s.radius)
        val n = outline.size; if (n < 3) return
        val cx = s.x + s.w * 0.5f; val cy = s.y + s.h * 0.5f
        val cp = tp(cx, cy)
        val cu = s.u0 + (s.u1 - s.u0) * 0.5f; val cv = s.v0 + (s.v1 - s.v0) * 0.5f
        for (i in 0 until n) {
            val j = (i + 1) % n
            val pi = outline[i]; val pj = outline[j]
            val ti = tp(pi.x, pi.y); val tj = tp(pj.x, pj.y)
            val ui = s.u0 + (pi.x - s.x) / s.w * (s.u1 - s.u0)
            val vi = s.v0 + (pi.y - s.y) / s.h * (s.v1 - s.v0)
            val uj = s.u0 + (pj.x - s.x) / s.w * (s.u1 - s.u0)
            val vj = s.v0 + (pj.y - s.y) / s.h * (s.v1 - s.v0)
            emit(cp.x, cp.y, cu, cv, color); emit(ti.x, ti.y, ui, vi, color); emit(tj.x, tj.y, uj, vj, color)
        }
    }

    private fun ensureInit() {
        if (initialized) return
        program = GLUtils.linkProgram("shaders/gl/lumina_tex.vsh", "shaders/gl/lumina_tex.fsh", "LuminaImg")
        uProjection = GL33C.glGetUniformLocation(program, "uProjection")
        uTexture = GL33C.glGetUniformLocation(program, "uTex")
        uMode = GL33C.glGetUniformLocation(program, "uMode")
        vao = GL33C.glGenVertexArrays(); vbo = GL33C.glGenBuffers()
        GL33C.glBindVertexArray(vao)
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo)
        GL33C.glBufferData(GL33C.GL_ARRAY_BUFFER, MAX_VERTS.toLong() * FLOATS * 4L, GL33C.GL_DYNAMIC_DRAW)
        val stride = FLOATS * 4
        GL33C.glEnableVertexAttribArray(0); GL33C.glVertexAttribPointer(0, 2, GL33C.GL_FLOAT, false, stride, 0L)
        GL33C.glEnableVertexAttribArray(1); GL33C.glVertexAttribPointer(1, 2, GL33C.GL_FLOAT, false, stride, 8L)
        GL33C.glEnableVertexAttribArray(2); GL33C.glVertexAttribPointer(2, 4, GL33C.GL_FLOAT, false, stride, 16L)
        GL33C.glBindVertexArray(0); GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, 0)
        initialized = true
    }

    private fun emit(x: Float, y: Float, u: Float, v: Float, c: FloatArray) {
        if (vCount >= MAX_VERTS) return
        buf.put(x).put(y).put(u).put(v).put(c[0]).put(c[1]).put(c[2]).put(c[3]); vCount++
    }

    fun destroy() {
        if (!initialized) return
        GL33C.glDeleteProgram(program); GL33C.glDeleteVertexArrays(vao); GL33C.glDeleteBuffers(vbo)
        program = 0; vao = 0; vbo = 0; initialized = false; tmpQuad.free()
    }
}
