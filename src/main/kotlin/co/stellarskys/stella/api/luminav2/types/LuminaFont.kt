package co.stellarskys.stella.api.luminav2.types

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.zenith.Zenith
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.resources.Identifier
import org.lwjgl.stb.STBTTAlignedQuad
import org.lwjgl.stb.STBTTFontinfo
import org.lwjgl.stb.STBTTPackContext
import org.lwjgl.stb.STBTTPackedchar
import org.lwjgl.stb.STBTruetype
import org.lwjgl.system.MemoryUtil

class LuminaFont(data: ByteArray, val config: FontConfig) {
    private val chars = STBTTPackedchar.malloc(config.numChars)
    private val quad = STBTTAlignedQuad.malloc()
    private var destroyed = false

    val atlas: LuminaImage
    val ascentPx: Float
    val descentPx: Float

    data class FontConfig(
        val bakeSize: Float, val oversample: Int,
        val atlasWidth: Int, val atlasHeight: Int,
        val firstChar: Int = 32, val numChars: Int = 96
    ) {
        companion object {
            val DEFAULT = FontConfig(48f, 2, 2048, 2048)
        }
    }

    data class GlyphQuad(
        val x0: Float, val y0: Float, val x1: Float, val y1: Float,
        val u0: Float, val v0: Float, val u1: Float, val v1: Float
    ) {
        companion object {
            fun fromAlinged(quad : STBTTAlignedQuad): GlyphQuad = GlyphQuad(
                quad.x0(), quad.y0(), quad.x1(), quad.y1(),
                quad.s0(), quad.t0(), quad.s1(), quad.t1()
            )
        }
    }

    init {
        val info = STBTTFontinfo.malloc()
        val buffer = MemoryUtil.memAlloc(data.size).put(data).flip()
        if (!STBTruetype.stbtt_InitFont(info, buffer)) error("Failed to init font")

        val scale = STBTruetype.stbtt_ScaleForPixelHeight(info, config.bakeSize)
        val ascent = IntArray(1)
        val descent = IntArray(1)
        val lineGap = IntArray(1)

        STBTruetype.stbtt_GetFontVMetrics(info, ascent, descent, lineGap)
        info.free()

        ascentPx = ascent[0] * scale
        descentPx = descent[0] * scale

        val context = STBTTPackContext.malloc()
        val atlasBuffer = MemoryUtil.memCalloc(config.atlasWidth * config.atlasHeight)

        STBTruetype.stbtt_PackBegin(context, atlasBuffer, config.atlasWidth, config.atlasHeight, 0, 3, 0L)
        STBTruetype.stbtt_PackSetOversampling(context, config.oversample, config.oversample)
        STBTruetype.stbtt_PackFontRange(context, buffer, 0, config.bakeSize, config.firstChar, chars)
        STBTruetype.stbtt_PackEnd(context)
        context.free()
        MemoryUtil.memFree(buffer)

        val image = NativeImage(config.atlasWidth, config.atlasHeight, true)

        for (y in 0 until config.atlasHeight) {
            for (x in 0 until config.atlasWidth) {
                val alpha = atlasBuffer.get(y * config.atlasWidth + x).toInt() and 0xFF
                val pixel = (alpha shl 24) or 0x00FFFFFF
                image.setPixel(x, y, pixel)
            }
        }

        atlas = LuminaImage(image, FilterMode.LINEAR)
        MemoryUtil.memFree(atlasBuffer)
    }

    fun getGlyphQuad(char: Char, xCursor: FloatArray, yCursor: FloatArray): GlyphQuad? {
        val charIndex = char.code - config.firstChar
        if (charIndex !in 0 until config.numChars) return null
        STBTruetype.stbtt_GetPackedQuad(chars, config.atlasWidth, config.atlasHeight, charIndex, xCursor, yCursor, quad, true)
        return GlyphQuad.fromAlinged(quad)
    }

    fun destroy() {
        if (destroyed) return

        chars.free()
        quad.free()
        atlas.destroy()
        destroyed = true
    }

   companion object {
        fun fromResource(path: String, config: FontConfig = FontConfig.DEFAULT): LuminaFont = fromResource(Identifier.fromNamespaceAndPath(Stella.NAMESPACE, path), config)
        fun fromResource(resource: Identifier, config: FontConfig = FontConfig.DEFAULT): LuminaFont {
            val stream = Zenith.resourceManager.getResource(resource)
                .orElseThrow { RuntimeException("Font not found: $resource") }
                .open()

            return LuminaFont(stream.readBytes(), config)
        }

        fun fromBytes(bytes: ByteArray, config: FontConfig = FontConfig.DEFAULT): LuminaFont = LuminaFont(bytes, config)
    }
}