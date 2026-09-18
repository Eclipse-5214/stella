package co.stellarskys.stella.api.luminav2

import co.stellarskys.stella.api.luminav2.render.LuminaShapeRenderer
import co.stellarskys.stella.api.luminav2.render.LuminaTextureRenderer
import co.stellarskys.stella.api.luminav2.render.LuminaV2PipRenderer
import co.stellarskys.stella.api.luminav2.types.Gradient
import co.stellarskys.stella.api.luminav2.types.GradientType
import co.stellarskys.stella.api.luminav2.types.LuminaFont
import co.stellarskys.stella.api.luminav2.types.LuminaImage
import co.stellarskys.stella.api.zenith.Zenith
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2f
import org.joml.Vector2f

object LuminaV2 {
    private val queue: MutableList<Entry> = mutableListOf()
    private val transformStack = ArrayDeque<Matrix3x2f>()
    private val scissorStack = ArrayDeque<ScissorRect>()
    private var currentTransform = Matrix3x2f()
    private var settingMask = false
    private var masking = false

    val dpr: Float get() {
        //? if > 26.2 {
        /*return Zenith.window.pixelDensity
        *///? } else {
        val fbw = Zenith.Res.viewportWidth.toFloat()
        val ww = Zenith.Res.windowWidth.toFloat()
        return if (ww == 0f) 1f else fbw / ww
        //? }
    }

    val mask: Mask get() = when  {
        settingMask -> Mask.Write
        masking -> Mask.In
        else -> Mask.None
    }

    val inter = LuminaFont.fromResource("font/montserrat.ttf")

    data class ScissorRect(val x: Float, val y: Float, val w: Float, val h: Float)
    open class Entry(val transform: Matrix3x2f, val scissor: ScissorRect?) {
        val mask = LuminaV2.mask

        fun toScreen(x: Float, y: Float, dpr: Float): Vector2f =
            transform.transformPosition(Vector2f(x, y)).mul(dpr)
    }

    enum class Mask { In, Write, None }

    fun flush(ctx: GuiGraphicsExtractor) {
        if(queue.isEmpty()) return
        val entries = queue.toList()
        queue.clear()
        LuminaV2PipRenderer.draw(ctx, entries)
    }

    // transformations
    fun push() { transformStack.addLast(Matrix3x2f(currentTransform)) }
    fun pop() { currentTransform = transformStack.removeLastOrNull() ?: Matrix3x2f() }
    fun translate(x: Float, y: Float) { currentTransform.translate(x, y) }
    fun scale(x: Float, y: Float) { currentTransform.scale(x, y) }
    fun rotate(radians: Float) { currentTransform.rotate(radians) }
    fun resetTransform() { currentTransform = Matrix3x2f() }
    fun setTransform(new: Matrix3x2f) { currentTransform.set(new) }

    fun pushPop(block: ()-> Unit) {
        push(); block(); pop()
    }

    // scissor stack
    fun pushScissor(x: Float, y: Float, w: Float, h: Float) {
        push()
        val p0 = currentTransform.transformPosition(Vector2f(x, y))
        val p1 = currentTransform.transformPosition(Vector2f(x + w, y + h))
        var sx = minOf(p0.x, p1.x); var sy = minOf(p0.y, p1.y)
        var sw = maxOf(p0.x, p1.x) - sx; var sh = maxOf(p0.y, p1.y) - sy
        val parent = scissorStack.lastOrNull()
        if (parent != null) {
            val nx = maxOf(sx, parent.x); val ny = maxOf(sy, parent.y)
            sw = maxOf(0f, minOf(sx + sw, parent.x + parent.w) - nx)
            sh = maxOf(0f, minOf(sy + sh, parent.y + parent.h) - ny)
            sx = nx; sy = ny
        }
        scissorStack.addLast(ScissorRect(sx, sy, sw, sh))
    }

    fun popScissor() { scissorStack.removeLastOrNull(); pop() }

    // masking
    fun pushMask(define: () -> Unit) {
        if (masking || settingMask) error("Already masking!")
        settingMask = true
        define()
        settingMask = false
        masking = true
    }

    fun popMask() {
        masking = false
    }

    fun pushPopMask(define: () -> Unit, block: () -> Unit) {
        pushMask { define() }; block(); popMask()
    }

    // shapes
    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, tl: Float, tr: Float, br: Float, bl: Float) {
        addShape(x, y, w, h, tl, tr, br, bl, 0f, color)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float = 0f) {
        rect(x, y, w, h, color, radius, radius, radius, radius)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float, roundTop: Boolean) {
        if (roundTop) rect(x, y, w, h, color, radius, radius, 0f, 0f)
        else rect(x, y, w, h, color, 0f, 0f, radius, radius)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, tl: Float, tr: Float, br: Float, bl: Float) {
        addShape(x, y, w, h, tl, tr, br, bl, thickness, color)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float = 0f) {
        hollowRect(x, y, w, h, thickness, color, radius, radius, radius, radius)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float, roundTop: Boolean) {
        if (roundTop) hollowRect(x, y, w, h, thickness, color, radius, radius, 0f, 0f)
        else hollowRect(x, y, w, h, thickness, color, 0f, 0f, radius, radius)
    }

    fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Int, color2: Int, gradientType: GradientType, radius: Float = 0f) {
        addShape(x, y, w, h, radius, radius, radius, radius, 0f, 0, Gradient(color1, color2, gradientType))
    }

    fun hollowGradientRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color1: Int, color2: Int, gradientType: GradientType, radius: Float = 0f) {
        addShape(x, y, w, h, radius, radius, radius, radius, thickness, 0, Gradient(color1, color2, gradientType))
    }

    private fun addShape(x: Float, y: Float, w: Float, h: Float, tl: Float, tr: Float, br: Float, bl: Float, border: Float, color: Int, grad: Gradient? = null) {
        queue.add(LuminaShapeRenderer.ShapeEntry(x, y, w, h, tl, tr, br, bl, border, color, Matrix3x2f(currentTransform), scissorStack.lastOrNull(), grad))
    }

    // textures
    fun image(image: LuminaImage, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f) {
        addImage(image, x, y, w, h, radius = radius)
    }

    fun image(image: LuminaImage, x: Float, y: Float, w: Float, h: Float, color: Int) {
        addImage(image, x, y, w, h, color)
    }

    fun image(image: LuminaImage, textureWidth: Int, textureHeight: Int, subX: Int, subY: Int, subW: Int, subH: Int, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f) {
        val u0 = subX.toFloat() / textureWidth; val v0 = subY.toFloat() / textureHeight
        val u1 = (subX + subW).toFloat() / textureWidth; val v1 = (subY + subH).toFloat() / textureHeight
        addImage(image, x, y, w, h, 0xFFFFFFFF.toInt(), radius, u0, v0, u1, v1)
    }

    private fun addImage(image: LuminaImage, x: Float, y: Float, w: Float, h: Float, color: Int = 0xFFFFFFFF.toInt(), radius: Float = 0f, u0: Float = 0f, v0: Float = 0f, u1: Float = 1f, v1: Float = 1f) {
        queue.add(LuminaTextureRenderer.ImageEntry(image, x, y, w, h, u0, v0, u1, v1, radius, color, Matrix3x2f(currentTransform), scissorStack.lastOrNull()))
    }

    // text
    fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: LuminaFont = inter) {
        if (text.isEmpty()) return
        queue.add(LuminaTextureRenderer.TextEntry(text, x, y, size, color, font, Matrix3x2f(currentTransform), scissorStack.lastOrNull()))
    }

    fun textWidth(text: String, size: Float, font: LuminaFont = inter): Float {
        if (text.isEmpty()) return 0f
        return font.textWidth(text, size)
    }
}