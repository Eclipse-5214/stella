package co.stellarskys.stella.utils.render.nvg

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.Utils.toHex
import co.stellarskys.stella.utils.render.nvg.Color.Companion.alpha
import co.stellarskys.stella.utils.render.nvg.Color.Companion.red
import co.stellarskys.stella.utils.render.nvg.Color.Companion.green
import co.stellarskys.stella.utils.render.nvg.Color.Companion.blue
import dev.deftu.omnicore.api.client.client
import net.minecraft.resources.ResourceLocation
import org.lwjgl.nanovg.NVGColor
import org.lwjgl.nanovg.NVGPaint
import org.lwjgl.nanovg.NanoSVG.*
import org.lwjgl.nanovg.NanoVG.*
import org.lwjgl.nanovg.NanoVGGL3.*
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryUtil.memAlloc
import org.lwjgl.system.MemoryUtil.memFree
import java.awt.Color
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round

/*
 * Adapted from NVGRenderer.kt in OdinFabric
 * https://github.com/odtheking/OdinFabric
 *
 * BSD 3-Clause License
 * Copyright (c) 2025, odtheking
 * See full license at: https://opensource.org/licenses/BSD-3-Clause
 */
object NVGRenderer {
    private val nvgPaint = NVGPaint.malloc()
    private val nvgColor = NVGColor.malloc()
    private val nvgColor2 = NVGColor.malloc()

    private val fontMap = HashMap<Font, NVGFont>()
    private val fontBounds = FloatArray(4)
    private val images = HashMap<Image, NVGImage>()

    private var scissor: Scissor? = null
    private var drawing: Boolean = false
    private var vg = -1L

    val inter = Font("Default", client.resourceManager.getResource(ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "font/montserrat.ttf")).get().open())

    init {
        vg = nvgCreate(NVG_ANTIALIAS or NVG_STENCIL_STROKES)
        require(vg != -1L) { "Failed to initialize NanoVG" }
    }

    fun beginFrame(width: Float, height: Float, devicePixelRatio: Float) {
        if (drawing) throw IllegalStateException("[NVGRenderer] Already drawing, but called beginFrame")

        nvgBeginFrame(vg, width, height, devicePixelRatio)
        nvgTextAlign(vg, NVG_ALIGN_LEFT or NVG_ALIGN_TOP)
        drawing = true
    }

    fun endFrame() {
        if (!drawing) throw IllegalStateException("[NVGRenderer] Not drawing, but called endFrame")
        nvgEndFrame(vg)

        drawing = false
    }

    fun push() = nvgSave(vg)

    fun pop() = nvgRestore(vg)

    fun scale(x: Float, y: Float) = nvgScale(vg, x, y)

    fun translate(x: Float, y: Float) = nvgTranslate(vg, x, y)

    fun rotate(amount: Float) = nvgRotate(vg, amount)

    fun globalAlpha(amount: Float) = nvgGlobalAlpha(vg, amount.coerceIn(0f, 1f))

    fun setTransform(m00: Float, m01: Float, m10: Float, m11: Float, m20: Float, m21: Float) {
        if (!drawing) return
        nvgTransform(vg, m00, m01, m10, m11, m20, m21)
    }

    // Optional but very helpful: resets the matrix to identity
    fun resetTransform() {
        if (!drawing) return
        nvgResetTransform(vg)
    }

    fun pushScissor(x: Float, y: Float, w: Float, h: Float) {
        // 1. We MUST save the state so that popScissor can restore the previous clip
        nvgSave(vg)

        // 2. If this is the first scissor, use nvgScissor.
        // If it's a nested scissor, use nvgIntersectScissor.
        if (scissor == null) {
            nvgScissor(vg, x, y, w, h)
        } else {
            nvgIntersectScissor(vg, x, y, w, h)
        }

        // 3. Keep the stack just to track depth/existence
        scissor = Scissor(scissor, x, y, x + w, y + h)
    }

    fun popScissor() {
        if (scissor == null) return

        // 1. Restore NanoVG state (this automatically pops the scissor/matrix)
        nvgRestore(vg)

        // 2. Move up the stack
        scissor = scissor?.previous
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
        nvgBeginPath(vg)
        nvgMoveTo(vg, x1, y1)
        nvgLineTo(vg, x2, y2)
        nvgStrokeWidth(vg, thickness)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float, roundTop: Boolean) {
        nvgBeginPath(vg)

        if (roundTop) {
            nvgMoveTo(vg, x, y + h)
            nvgLineTo(vg, x + w, y + h)
            nvgLineTo(vg, x + w, y + radius)
            nvgArcTo(vg, x + w, y, x + w - radius, y, radius)
            nvgLineTo(vg, x + radius, y)
            nvgArcTo(vg, x, y, x, y + radius, radius)
            nvgLineTo(vg, x, y + h)
        } else {
            nvgMoveTo(vg, x, y)
            nvgLineTo(vg, x + w, y)
            nvgLineTo(vg, x + w, y + h - radius)
            nvgArcTo(vg, x + w, y + h, x + w - radius, y + h, radius)
            nvgLineTo(vg, x + radius, y + h)
            nvgArcTo(vg, x, y + h, x, y + h - radius, radius)
            nvgLineTo(vg, x, y)
        }

        nvgClosePath(vg)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int) {
        nvgBeginPath(vg)
        nvgRect(vg, x, y, w, h)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, topRight: Float, topLeft: Float, bottomRight: Float, bottomLeft: Float) {
        nvgBeginPath(vg)
        nvgRoundedRectVarying(vg, round(x), round(y), round(w), round(h), topRight, topLeft, bottomRight, bottomLeft)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float, roundTop: Boolean) {
        nvgBeginPath(vg)

        if (roundTop) {
            nvgMoveTo(vg, x, y + h)
            nvgLineTo(vg, x + w, y + h)
            nvgLineTo(vg, x + w, y + radius)
            nvgArcTo(vg, x + w, y, x + w - radius, y, radius)
            nvgLineTo(vg, x + radius, y)
            nvgArcTo(vg, x, y, x, y + radius, radius)
            nvgLineTo(vg, x, y + h)
        } else {
            nvgMoveTo(vg, x, y)
            nvgLineTo(vg, x + w, y)
            nvgLineTo(vg, x + w, y + h - radius)
            nvgArcTo(vg, x + w, y + h, x + w - radius, y + h, radius)
            nvgLineTo(vg, x + radius, y + h)
            nvgArcTo(vg, x, y + h, x, y + h - radius, radius)
            nvgLineTo(vg, x, y)
        }

        nvgClosePath(vg)
        nvgStrokeWidth(vg, thickness)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }


    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, topRight: Float, topLeft: Float, bottomRight: Float, bottomLeft: Float) {
        nvgBeginPath(vg)
        nvgRoundedRectVarying(vg, round(x), round(y), round(w), round(h), topRight, topLeft, bottomRight, bottomLeft)
        nvgStrokeWidth(vg, thickness)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        nvgStrokeWidth(vg, thickness)
        nvgPathWinding(vg, NVG_HOLE)
        color(color)
        nvgStrokeColor(vg, nvgColor)
        nvgStroke(vg)
    }

    fun hollowGradientRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        thickness: Float,
        color1: Int,
        color2: Int,
        gradient: Gradient,
        radius: Float
    ) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        nvgStrokeWidth(vg, thickness)

        // Gradient stroke
        gradient(color1, color2, x, y, w, h, gradient)
        nvgStrokePaint(vg, nvgPaint)
        nvgStroke(vg)
    }

    fun gradientRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color1: Int,
        color2: Int,
        gradient: Gradient,
        radius: Float = 0f
    ) {
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        gradient(color1, color2, x, y, w, h, gradient)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun gradientRect(
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        color1: Int,
        color2: Int,
        gradient: Gradient,
        topRight: Float,
        topLeft: Float,
        bottomRight: Float,
        bottomLeft: Float
    ) {
        nvgBeginPath(vg)
        // Use the varying version to support individual corner rounding
        nvgRoundedRectVarying(vg, round(x), round(y), round(w), round(h), topLeft, topRight, bottomRight, bottomLeft)

        gradient(color1, color2, x, y, w, h, gradient)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun dropShadow(x: Float, y: Float, width: Float, height: Float, blur: Float, spread: Float, shadowColor: Color, radius: Float) {
        val r = shadowColor.red.toByte()
        val g = shadowColor.green.toByte()
        val b = shadowColor.blue.toByte()

        nvgRGBA(r, g, b, 125, nvgColor)
        nvgRGBA(r, g, b, 0, nvgColor2)

        nvgBoxGradient(
            vg,
            x - spread,
            y - spread,
            width + 2 * spread,
            height + 2 * spread,
            radius + spread,
            blur,
            nvgColor,
            nvgColor2,
            nvgPaint
        )
        nvgBeginPath(vg)
        nvgRoundedRect(
            vg,
            x - spread - blur,
            y - spread - blur,
            width + 2 * spread + 2 * blur,
            height + 2 * spread + 2 * blur,
            radius + spread
        )
        nvgRoundedRect(vg, x, y, width, height, radius)
        nvgPathWinding(vg, NVG_HOLE)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun circle(x: Float, y: Float, radius: Float, color: Int) {
        nvgBeginPath(vg)
        nvgCircle(vg, x, y, radius)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
    }

    fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: Font) {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontID(font))
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x, y + .5f, text)
    }

    fun shadowedText(text: String, x: Float, y: Float, size: Float, color: Int, font: Font, shadowColor: Int, offsetX: Float, offsetY: Float, blur: Float) {
        nvgFontFaceId(vg, getFontID(font))
        nvgFontSize(vg, size)

        nvgFontBlur(vg, blur)
        color(shadowColor)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x + offsetX, y + offsetY, text)

        nvgFontBlur(vg, 0f)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgText(vg, x, y + .5f, text)
    }

    fun textWidth(text: String, size: Float, font: Font): Float {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontID(font))
        return nvgTextBounds(vg, 0f, 0f, text, fontBounds)
    }

    fun wrappedText(
        text: String,
        x: Float,
        y: Float,
        w: Float,
        size: Float,
        color: Int,
        font: Font,
        lineHeight: Float
    ) {
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontID(font))
        nvgTextLineHeight(vg, lineHeight)
        color(color)
        nvgFillColor(vg, nvgColor)
        nvgTextBox(vg, x, y, w, text)
    }

    fun textBounds(
        text: String,
        w: Float,
        size: Float,
        font: Font,
        lineHeight: Float
    ): FloatArray {
        val bounds = FloatArray(4)
        nvgFontSize(vg, size)
        nvgFontFaceId(vg, getFontID(font))
        nvgTextLineHeight(vg, lineHeight)
        nvgTextBoxBounds(vg, 0f, 0f, w, text, bounds)
        return bounds // [minX, minY, maxX, maxY]
    }

    fun createNVGImage(textureId: Int, textureWidth: Int, textureHeight: Int): Int =
        nvglCreateImageFromHandle(vg, textureId, textureWidth, textureHeight, NVG_IMAGE_NEAREST or NVG_IMAGE_NODELETE)

    fun image(image: Int, textureWidth: Int, textureHeight: Int, subX: Int, subY: Int, subW: Int, subH: Int, x: Float, y: Float, w: Float, h: Float, radius: Float) {
        if (image == -1) return

        val sx = subX.toFloat() / textureWidth
        val sy = subY.toFloat() / textureHeight
        val sw = subW.toFloat() / textureWidth
        val sh = subH.toFloat() / textureHeight

        val iw = w / sw
        val ih = h / sh
        val ix = x - iw * sx
        val iy = y - ih * sy

        nvgImagePattern(vg, ix, iy, iw, ih, 0f, image, 1f, nvgPaint)
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h + .5f, radius)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun image(image: Image, x: Float, y: Float, w: Float, h: Float, radius: Float) {
        nvgImagePattern(vg, x, y, w, h, 0f, getImage(image), 1f, nvgPaint)
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h + .5f, radius)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun image(image: Image, x: Float, y: Float, w: Float, h: Float) {
        nvgImagePattern(vg, x, y, w, h, 0f, getImage(image), 1f, nvgPaint)
        nvgBeginPath(vg)
        nvgRect(vg, x, y, w, h + .5f)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun image(image: Image, x: Float, y: Float, w: Float, h: Float, color: Int) {
        nvgImagePattern(vg, x, y, w, h, 0f, getImage(image), 1f, nvgPaint)
        color(color)
        nvgPaint.innerColor(nvgColor)
        nvgPaint.outerColor(nvgColor)

        nvgBeginPath(vg)
        nvgRect(vg, x, y, w, h + .5f)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun svg(id: String, x: Float, y: Float, w: Float, h: Float, a: Float) {
        val nvg = getImage(Image(id))

        nvgImagePattern(vg, x, y, w, h, 0f, nvg, a, nvgPaint)
        nvgBeginPath(vg)
        nvgRect(vg, x, y, w, h + .5f)
        nvgFillPaint(vg, nvgPaint)
        nvgFill(vg)
    }

    fun createImage(resourcePath: String): Image {
        val image = images.keys.find { it.identifier == resourcePath } ?: Image(resourcePath)
        images.getOrPut(image) {
            val nvgId = if (image.isSVG) loadSVG(image) else loadImage(image)
            NVGImage(0, nvgId)
        }.count++
        return image
    }

    fun cleanCache() {
        val iter = images.entries.iterator()
        while (iter.hasNext()) {
            val entry = iter.next()
            nvgDeleteImage(vg, entry.value.nvg)
            iter.remove()
        }
    }

    // lowers reference count by 1, if it reaches 0 it gets deleted from mem
    fun deleteImage(image: Image) {
        val nvgImage = images[image] ?: return
        nvgImage.count--
        if (nvgImage.count == 0) {
            nvgDeleteImage(vg, nvgImage.nvg)
            images.remove(image)
        }
    }

    private fun getImage(image: Image): Int {
        return images[image]?.nvg ?: throw IllegalStateException("Image (${image.identifier}) doesn't exist")
    }

    private fun loadImage(image: Image): Int {
        val w = IntArray(1)
        val h = IntArray(1)
        val channels = IntArray(1)
        val buffer = STBImage.stbi_load_from_memory(
            image.buffer(),
            w,
            h,
            channels,
            4
        ) ?: throw NullPointerException("Failed to load image: ${image.identifier}")
        return nvgCreateImageRGBA(vg, w[0], h[0], 0, buffer)
    }

    private fun loadSVG(image: Image): Int {
        val vec = image.stream.use { it.bufferedReader().readText() }
        val svg = nsvgParse(vec, "px", 96f) ?: throw IllegalStateException("Failed to parse ${image.identifier}")

        val width = svg.width().toInt()
        val height = svg.height().toInt()
        val buffer = memAlloc(width * height * 4)

        try {
            val rasterizer = nsvgCreateRasterizer()
            nsvgRasterize(rasterizer, svg, 0f, 0f, 1f, buffer, width, height, width * 4)
            val nvgImage = nvgCreateImageRGBA(vg, width, height, 0, buffer)
            nsvgDeleteRasterizer(rasterizer)
            return nvgImage
        } finally {
            nsvgDelete(svg)
            memFree(buffer)
        }
    }


    private fun color(color: Int) {
        nvgRGBA(color.red.toByte(), color.green.toByte(), color.blue.toByte(), color.alpha.toByte(), nvgColor)
    }

    private fun color(color1: Int, color2: Int) {
        nvgRGBA(
            color1.red.toByte(),
            color1.green.toByte(),
            color1.blue.toByte(),
            color1.alpha.toByte(),
            nvgColor
        )
        nvgRGBA(
            color2.red.toByte(),
            color2.green.toByte(),
            color2.blue.toByte(),
            color2.alpha.toByte(),
            nvgColor2
        )
    }

    private fun gradient(color1: Int, color2: Int, x: Float, y: Float, w: Float, h: Float, direction: Gradient) {
        color(color1, color2)
        when (direction) {
            Gradient.LeftToRight -> nvgLinearGradient(vg, x, y, x + w, y, nvgColor, nvgColor2, nvgPaint)
            Gradient.TopToBottom -> nvgLinearGradient(vg, x, y, x, y + h, nvgColor, nvgColor2, nvgPaint)
            Gradient.TopLeftToBottomRight -> nvgLinearGradient(vg, x, y, x + w, y + h, nvgColor, nvgColor2, nvgPaint)
        }
    }

    private fun getFontID(font: Font): Int {
        return fontMap.getOrPut(font) {
            val buffer = font.buffer()
            NVGFont(nvgCreateFontMem(
                vg,
                font.name,
                buffer,
                false
            ), buffer)
        }.id
    }

    private class Scissor(val previous: Scissor?, val x: Float, val y: Float, val maxX: Float, val maxY: Float)

    fun drawMasked(x: Float, y: Float, w: Float, h: Float, radius: Float, block: () -> Unit) {
        push()
        nvgBeginPath(vg)
        nvgRoundedRect(vg, x, y, w, h, radius)
        nvgRGBA(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte(), nvgColor)
        nvgFillColor(vg, nvgColor)
        nvgFill(vg)
        nvgGlobalCompositeOperation(vg, NVG_SOURCE_IN)
        block()
        pop()
    }

    private data class NVGImage(var count: Int, val nvg: Int)
    private data class NVGFont(val id: Int, val buffer: ByteBuffer)
}