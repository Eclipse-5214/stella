package co.stellarskys.stella.api.lumina.renderer.gl

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.lumina.Lumina
import net.minecraft.resources.Identifier
import org.lwjgl.opengl.GL33C
import org.lwjgl.system.MemoryUtil
import java.nio.FloatBuffer

internal object GLUtils {
    fun unpackPremultiplied(argb: Int): FloatArray {
        val a = ((argb ushr 24) and 0xFF) / 255f
        val r = ((argb ushr 16) and 0xFF) / 255f
        val g = ((argb ushr 8) and 0xFF) / 255f
        val b = (argb and 0xFF) / 255f
        return floatArrayOf(r * a, g * a, b * a, a)
    }

    fun orthoProjection(w: Int, h: Int) = floatArrayOf(
        2f / w, 0f, 0f, 0f, 0f, -2f / h, 0f, 0f, 0f, 0f, -1f, 0f, -1f, 1f, 0f, 1f
    )

    fun uploadVertices(vbo: Int, buf: FloatBuffer, count: Int, floatsPerVertex: Int) {
        GL33C.glBindBuffer(GL33C.GL_ARRAY_BUFFER, vbo)
        buf.position(0).limit(count * floatsPerVertex)
        GL33C.nglBufferSubData(GL33C.GL_ARRAY_BUFFER, 0, count.toLong() * floatsPerVertex * 4L, MemoryUtil.memAddress(buf))
    }

    fun applyScissor(scissor: Lumina.ScissorRect?, viewportHeight: Int) {
        if (scissor != null) {
            GL33C.glEnable(GL33C.GL_SCISSOR_TEST)
            GL33C.glScissor(scissor.x.toInt(), viewportHeight - scissor.y.toInt() - scissor.h.toInt(), scissor.w.toInt(), scissor.h.toInt())
        } else {
            GL33C.glDisable(GL33C.GL_SCISSOR_TEST)
        }
    }

    fun setPixelStoreDefaults() {
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ALIGNMENT, 1)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_ROW_LENGTH, 0)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_PIXELS, 0)
        GL33C.glPixelStorei(GL33C.GL_UNPACK_SKIP_ROWS, 0)
    }

    fun linkProgram(vsPath: String, fsPath: String, label: String): Int {
        val vs = compileShader(GL33C.GL_VERTEX_SHADER, loadShaderSource(vsPath), label)
        val fs = compileShader(GL33C.GL_FRAGMENT_SHADER, loadShaderSource(fsPath), label)
        val prog = GL33C.glCreateProgram()
        GL33C.glAttachShader(prog, vs); GL33C.glAttachShader(prog, fs)
        GL33C.glLinkProgram(prog)
        if (GL33C.glGetProgrami(prog, GL33C.GL_LINK_STATUS) == GL33C.GL_FALSE) {
            val log = GL33C.glGetProgramInfoLog(prog); GL33C.glDeleteProgram(prog)
            throw RuntimeException("$label link failed: $log")
        }
        GL33C.glDeleteShader(vs); GL33C.glDeleteShader(fs)
        return prog
    }

    private fun loadShaderSource(path: String): String {
        val id = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, path)
        return net.minecraft.client.Minecraft.getInstance().resourceManager.getResource(id)
            .orElseThrow { RuntimeException("Missing shader: $id") }
            .open().bufferedReader().readText()
    }

    private fun compileShader(type: Int, source: String, label: String): Int {
        val shader = GL33C.glCreateShader(type)
        GL33C.glShaderSource(shader, source); GL33C.glCompileShader(shader)
        if (GL33C.glGetShaderi(shader, GL33C.GL_COMPILE_STATUS) == GL33C.GL_FALSE) {
            val log = GL33C.glGetShaderInfoLog(shader); GL33C.glDeleteShader(shader)
            throw RuntimeException("$label shader compile failed: $log")
        }
        return shader
    }

    fun saveGLState(): GLState {
        val cm = IntArray(4)
        GL33C.glGetIntegerv(GL33C.GL_COLOR_WRITEMASK, cm)
        return GLState(
            GL33C.glGetInteger(GL33C.GL_CURRENT_PROGRAM), GL33C.glGetInteger(GL33C.GL_VERTEX_ARRAY_BINDING),
            GL33C.glIsEnabled(GL33C.GL_BLEND), GL33C.glIsEnabled(GL33C.GL_SCISSOR_TEST),
            GL33C.glIsEnabled(GL33C.GL_DEPTH_TEST), GL33C.glIsEnabled(GL33C.GL_CULL_FACE),
            GL33C.glIsEnabled(GL33C.GL_STENCIL_TEST),
            GL33C.glGetInteger(GL33C.GL_BLEND_SRC_RGB), GL33C.glGetInteger(GL33C.GL_BLEND_DST_RGB),
            GL33C.glGetInteger(GL33C.GL_BLEND_SRC_ALPHA), GL33C.glGetInteger(GL33C.GL_BLEND_DST_ALPHA),
            GL33C.glGetInteger(GL33C.GL_TEXTURE_BINDING_2D),
            cm[0] != 0, cm[1] != 0, cm[2] != 0, cm[3] != 0
        )
    }

    data class GLState(
        val prog: Int, val vao: Int, val blend: Boolean, val scissor: Boolean,
        val depth: Boolean, val cull: Boolean, val stencil: Boolean,
        val blendSrc: Int, val blendDst: Int, val blendSrcA: Int, val blendDstA: Int,
        val tex: Int, val cmR: Boolean, val cmG: Boolean, val cmB: Boolean, val cmA: Boolean
    ) {
        fun restore() {
            GL33C.glUseProgram(prog); GL33C.glBindVertexArray(vao)
            GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, tex)
            GL33C.glBlendFuncSeparate(blendSrc, blendDst, blendSrcA, blendDstA)
            GL33C.glColorMask(cmR, cmG, cmB, cmA)
            if (!blend) GL33C.glDisable(GL33C.GL_BLEND)
            if (depth) GL33C.glEnable(GL33C.GL_DEPTH_TEST)
            if (cull) GL33C.glEnable(GL33C.GL_CULL_FACE)
            if (stencil) GL33C.glEnable(GL33C.GL_STENCIL_TEST) else GL33C.glDisable(GL33C.GL_STENCIL_TEST)
            if (scissor) GL33C.glEnable(GL33C.GL_SCISSOR_TEST) else GL33C.glDisable(GL33C.GL_SCISSOR_TEST)
        }
    }
}
