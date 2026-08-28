package co.stellarskys.stella.api.luminav2.types

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.zenith.Zenith
import com.github.weisj.jsvg.parser.LoaderContext
import com.github.weisj.jsvg.parser.SVGLoader
import com.github.weisj.jsvg.view.ViewBox
import com.mojang.blaze3d.platform.NativeImage
import com.mojang.blaze3d.textures.FilterMode
import net.minecraft.resources.Identifier
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.net.URI

class LuminaSvg(native: NativeImage): LuminaImage(native, FilterMode.LINEAR) {
    companion object {
        fun fromResource(path: String, scale: Float = 4f): LuminaSvg = fromResource(Identifier.fromNamespaceAndPath(Stella.NAMESPACE, path), scale)
        fun fromResource(resource: Identifier, scale: Float = 4f): LuminaSvg {
            val stream = Zenith.resourceManager.getResource(resource)
                .orElseThrow { RuntimeException("Image not found: $resource") }
                .open()

            return fromStream(stream, scale)
        }

        fun fromBytes(bytes: ByteArray, scale: Float = 4f): LuminaSvg = ByteArrayInputStream(bytes).use { stream -> fromStream(stream, scale) }

        private fun fromStream(stream: InputStream, scale: Float): LuminaSvg {
            val svg = SVGLoader().load(stream, URI(""), LoaderContext.createDefault() ) ?: error("Could not load svg")

            val size = svg.size()
            val width = (size.width * scale).toInt()
            val height = (size.height * scale).toInt()

            val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()

            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
            svg.render(null, graphics, ViewBox(width.toFloat(), height.toFloat()))
            graphics.dispose()

            val native = NativeImage(width, height, true)
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val pixel = image.getRGB(x, y)
                    native.setPixel(x, y, pixel)
                }
            }

            return LuminaSvg(native)
        }
    }
}