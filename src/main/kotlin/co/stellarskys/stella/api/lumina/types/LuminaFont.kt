package co.stellarskys.stella.api.lumina.types

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import co.stellarskys.stella.api.zenith.Zenith
import net.minecraft.resources.Identifier
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTPackContext
import org.lwjgl.stb.STBTTPackedchar
import org.lwjgl.stb.STBTruetype.*
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer

class LuminaFont(val name: String, private val resourcePath: String) {
    companion object {
        internal const val BAKE_SIZE = 128f
        private const val ATLAS_W = 2048
        private const val ATLAS_H = 2048
        internal const val FIRST_CHAR = 32
        internal const val NUM_CHARS = 96
        private const val OVERSAMPLE = 2
    }

    private var fontData: ByteBuffer? = null
    internal var packedChars: STBTTPackedchar.Buffer? = null
    internal var ascentPx = 0f
    private var atlasBitmap: ByteBuffer? = null
    internal var atlasTexture = 0
    private var baked = false
    private var textureUploaded = false

    private val tmpQuad = STBTTAlignedQuad.malloc()
    private val tmpXPos = floatArrayOf(0f)
    private val tmpYPos = floatArrayOf(0f)

    fun ensureBaked() {
        if (baked) return

        val bytes = Zenith.resourceManager
            .getResource(Identifier.fromNamespaceAndPath(Stella.NAMESPACE, resourcePath))
            .orElseThrow { RuntimeException("Missing font: $resourcePath") }
            .open().use { it.readBytes() }
        fontData = MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer

        val fontInfo = STBTTFontinfo.malloc()
        stbtt_InitFont(fontInfo, fontData!!)
        val ascent = IntArray(1)
        val descent = IntArray(1)
        val lineGap = IntArray(1)
        stbtt_GetFontVMetrics(fontInfo, ascent, descent, lineGap)
        val lineh = (ascent[0] - descent[0] + lineGap[0]).toFloat()
        ascentPx = ascent[0].toFloat() * BAKE_SIZE / lineh
        fontInfo.free()

        val packCtx = STBTTPackContext.malloc()
        atlasBitmap = MemoryUtil.memCalloc(ATLAS_W * ATLAS_H)
        packedChars = STBTTPackedchar.malloc(NUM_CHARS)

        stbtt_PackBegin(packCtx, atlasBitmap!!, ATLAS_W, ATLAS_H, 0, 8, 0L)
        stbtt_PackSetOversampling(packCtx, OVERSAMPLE, OVERSAMPLE)
        stbtt_PackFontRange(packCtx, fontData!!, 0, -BAKE_SIZE, FIRST_CHAR, packedChars!!)
        stbtt_PackEnd(packCtx)
        packCtx.free()

        baked = true
    }

    fun ensureTextureUploaded() {
        if (textureUploaded) return
        ensureBaked()

        atlasTexture = Lumina.backend.uploadTexture(ATLAS_W, ATLAS_H, atlasBitmap!!, LuminaBackend.TextureFormat.R8, mipmap = true)
        MemoryUtil.memFree(atlasBitmap)
        atlasBitmap = null
        textureUploaded = true
    }

    fun textWidth(text: String, size: Float): Float {
        ensureBaked()
        tmpXPos[0] = 0f; tmpYPos[0] = 0f
        for (c in text) {
            val ci = c.code - FIRST_CHAR
            if (ci !in 0..<NUM_CHARS) continue
            stbtt_GetPackedQuad(packedChars!!, ATLAS_W, ATLAS_H, ci, tmpXPos, tmpYPos, tmpQuad, false)
        }
        return tmpXPos[0] * (size / BAKE_SIZE)
    }

    internal fun getPackedQuad(charIndex: Int, xpos: FloatArray, ypos: FloatArray, quad: STBTTAlignedQuad) {
        stbtt_GetPackedQuad(packedChars!!, ATLAS_W, ATLAS_H, charIndex, xpos, ypos, quad, false)
    }

    fun destroy() {
        if (textureUploaded) {
            Lumina.backend.deleteTexture(atlasTexture)
            atlasTexture = 0
            textureUploaded = false
        }
        packedChars?.free(); packedChars = null
        atlasBitmap?.let { MemoryUtil.memFree(it) }; atlasBitmap = null
        fontData?.let { MemoryUtil.memFree(it) }; fontData = null
        tmpQuad.free()
        baked = false
    }

    override fun equals(other: Any?) = other is LuminaFont && name == other.name
    override fun hashCode() = name.hashCode()
}
