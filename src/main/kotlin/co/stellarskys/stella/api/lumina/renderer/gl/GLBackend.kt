package co.stellarskys.stella.api.lumina.renderer.gl

import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import com.mojang.blaze3d.opengl.GlConst
import com.mojang.blaze3d.opengl.GlStateManager
import org.lwjgl.opengl.GL33C
import java.nio.ByteBuffer

object GLBackend : LuminaBackend {
    override fun renderShapes(shapes: List<Lumina.QueuedShape>, vw: Int, vh: Int) = GLShapeRenderer.render(shapes, vw, vh)
    override fun renderTextured(text: List<LuminaBackend.TextEntry>, images: List<LuminaBackend.ImageEntry>, vw: Int, vh: Int) = GLTextureRenderer.render(text, images, vw, vh)

    override fun uploadTexture(width: Int, height: Int, data: ByteBuffer, format: LuminaBackend.TextureFormat, mipmap: Boolean): Int {
        val (internalFmt, glFmt) = when (format) {
            LuminaBackend.TextureFormat.RGBA -> GL33C.GL_RGBA8 to GL33C.GL_RGBA
            LuminaBackend.TextureFormat.R8 -> GL33C.GL_R8 to GL33C.GL_RED
        }
        val tex = GL33C.glGenTextures()
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, tex)
        GLUtils.setPixelStoreDefaults()
        GL33C.glTexImage2D(GL33C.GL_TEXTURE_2D, 0, internalFmt, width, height, 0, glFmt, GL33C.GL_UNSIGNED_BYTE, data)
        if (mipmap) {
            GL33C.glGenerateMipmap(GL33C.GL_TEXTURE_2D)
            GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR_MIPMAP_LINEAR)
        } else {
            GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MIN_FILTER, GL33C.GL_LINEAR)
        }
        GL33C.glTexParameteri(GL33C.GL_TEXTURE_2D, GL33C.GL_TEXTURE_MAG_FILTER, GL33C.GL_LINEAR)
        GL33C.glBindTexture(GL33C.GL_TEXTURE_2D, 0)
        return tex
    }

    override fun deleteTexture(id: Int) { GL33C.glDeleteTextures(id) }

    override fun setupRenderTarget(fboId: Int, width: Int, height: Int) {
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, fboId)
        GlStateManager._viewport(0, 0, width, height)
        GL33C.glBindSampler(0, 0)
    }

    override fun resetAfterRender() {
        GlStateManager._disableDepthTest()
        GlStateManager._disableCull()
        GlStateManager._enableBlend(/*? if >= 26.2 { */ /*0 *//*? } */)
        GlStateManager._blendFuncSeparate(770, 771, 1, 0)
    }

    override fun destroy() { GLShapeRenderer.destroy(); GLTextureRenderer.destroy() }
}
