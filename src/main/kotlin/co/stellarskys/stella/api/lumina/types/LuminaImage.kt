package co.stellarskys.stella.api.lumina.types

import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class LuminaImage(
    val width: Int,
    val height: Int,
    var textureId: Int = 0,
    private var rgbaData: ByteBuffer? = null,
    private val ownsTexture: Boolean = true
) {
    fun ensureUploaded() {
        if (textureId != 0) return
        val data = rgbaData ?: return
        textureId = Lumina.backend.uploadTexture(width, height, data, LuminaBackend.TextureFormat.RGBA)
        MemoryUtil.memFree(data)
        rgbaData = null
    }

    fun destroy() {
        if (ownsTexture && textureId != 0) {
            Lumina.backend.deleteTexture(textureId)
            textureId = 0
        }
        rgbaData?.let { MemoryUtil.memFree(it) }
        rgbaData = null
    }
}
