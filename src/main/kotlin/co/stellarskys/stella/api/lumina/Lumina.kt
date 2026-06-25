package co.stellarskys.stella.api.lumina

import co.stellarskys.stella.api.lumina.renderer.LuminaBackend
import co.stellarskys.stella.api.lumina.renderer.gl.GLBackend
import co.stellarskys.stella.api.lumina.types.LuminaFont
import co.stellarskys.stella.api.lumina.types.LuminaImage
import co.stellarskys.stella.api.lumina.types.LuminaSvg
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import com.mojang.blaze3d.platform.NativeImage
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.joml.Matrix3x2f
import org.joml.Vector2f
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.math.atan2
import kotlin.math.sqrt

//? if >= 26.2 {
/*import co.stellarskys.stella.mixins.accessors.AccessorGpuDevice
import co.stellarskys.stella.api.lumina.renderer.vk.VKBackend
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vulkan.VulkanDevice
*///? }

object Lumina {
    internal val backend: LuminaBackend by lazy {
        //? if >= 26.2 {
        /*val device = RenderSystem.getDevice() as? AccessorGpuDevice
        if (device?.getBackend() is VulkanDevice) VKBackend else GLBackend
        *///? } else {
        GLBackend
        //? }
    }
    private val shapeBatch = mutableListOf<QueuedShape>()
    private val textBatch = mutableListOf<LuminaBackend.TextEntry>()
    private val imageBatch = mutableListOf<LuminaBackend.ImageEntry>()
    private val drawOrder = mutableListOf<Int>()
    private val transformStack = ArrayDeque<Matrix3x2f>()
    private val scissorStack = ArrayDeque<ScissorRect>()
    private var currentTransform = Matrix3x2f()
    private var alpha = 1f
    private val imageCache = HashMap<String, LuminaImage>()
    private val wrappedTextures = HashMap<Int, LuminaImage>()
    private var nextWrappedId = -1000

    val inter = LuminaFont("Inter", "font/montserrat.ttf")

    val dpr: Float get() {
        val fbw = Zenith.Res.viewportWidth.toFloat()
        val ww = Zenith.Res.windowWidth.toFloat()
        return if (ww == 0f) 1f else fbw / ww
    }

    enum class Gradient { LeftToRight, TopToBottom, TopLeftToBottomRight }

    data class ScissorRect(val x: Float, val y: Float, val w: Float, val h: Float)

    data class QueuedShape(
        val x: Float, val y: Float, val w: Float, val h: Float,
        val tl: Float, val tr: Float, val br: Float, val bl: Float,
        val border: Float, val color: Int,
        val gradType: Int, val gradC1: Int, val gradC2: Int,
        val transform: Matrix3x2f, val scissor: ScissorRect?,
        val stencilOp: Int = 0
    )

    init {
        EventBus.on<GameEvent.Stop>{ backend.destroy() }
    }

    fun push() { transformStack.addLast(Matrix3x2f(currentTransform)) }
    fun pop() { currentTransform = transformStack.removeLastOrNull() ?: Matrix3x2f() }
    fun translate(x: Float, y: Float) { currentTransform.translate(x, y) }
    fun scale(x: Float, y: Float) { currentTransform.scale(x, y) }
    fun rotate(radians: Float) { currentTransform.rotate(radians) }
    fun globalAlpha(a: Float) { alpha = a.coerceIn(0f, 1f) }
    fun resetTransform() { currentTransform = Matrix3x2f() }
    fun setTransform(m: Matrix3x2f) { currentTransform.set(m) }

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


    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, tl: Float, tr: Float, br: Float, bl: Float) {
        addShape(x, y, w, h, tl, tr, br, bl, 0f, applyAlpha(color))
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float = 0f) {
        rect(x, y, w, h, color, radius, radius, radius, radius)
    }

    fun rect(x: Float, y: Float, w: Float, h: Float, color: Int, radius: Float, roundTop: Boolean) {
        if (roundTop) rect(x, y, w, h, color, radius, radius, 0f, 0f)
        else rect(x, y, w, h, color, 0f, 0f, radius, radius)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, tl: Float, tr: Float, br: Float, bl: Float) {
        addShape(x, y, w, h, tl, tr, br, bl, thickness, applyAlpha(color))
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float = 0f) {
        hollowRect(x, y, w, h, thickness, color, radius, radius, radius, radius)
    }

    fun hollowRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color: Int, radius: Float, roundTop: Boolean) {
        if (roundTop) hollowRect(x, y, w, h, thickness, color, radius, radius, 0f, 0f)
        else hollowRect(x, y, w, h, thickness, color, 0f, 0f, radius, radius)
    }

    fun gradientRect(x: Float, y: Float, w: Float, h: Float, color1: Int, color2: Int, gradient: Gradient, radius: Float = 0f) {
        val c1 = applyAlpha(color1); val c2 = applyAlpha(color2)
        addShape(x, y, w, h, radius, radius, radius, radius, 0f, c1, gradient.ordinal + 1, c1, c2)
    }

    fun hollowGradientRect(x: Float, y: Float, w: Float, h: Float, thickness: Float, color1: Int, color2: Int, gradient: Gradient, radius: Float = 0f) {
        val c1 = applyAlpha(color1); val c2 = applyAlpha(color2)
        addShape(x, y, w, h, radius, radius, radius, radius, thickness, c1, gradient.ordinal + 1, c1, c2)
    }

    fun drawMasked(x: Float, y: Float, w: Float, h: Float, radius: Float, block: () -> Unit) {
        shapeBatch.add(QueuedShape(x, y, w, h, radius, radius, radius, radius, 0f, -1, 0, 0, 0, Matrix3x2f(currentTransform), scissorStack.lastOrNull(), stencilOp = 1))
        drawOrder.add(0)
        block()
        shapeBatch.add(QueuedShape(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0, 0, 0, 0, Matrix3x2f(), null, stencilOp = 2))
        drawOrder.add(0)
    }

    fun circle(x: Float, y: Float, radius: Float, color: Int) {
        val c = applyAlpha(color); val d = radius * 2f
        addShape(x - radius, y - radius, d, d, radius, radius, radius, radius, 0f, c)
    }

    fun line(x1: Float, y1: Float, x2: Float, y2: Float, thickness: Float, color: Int) {
        val c = applyAlpha(color)
        val dx = x2 - x1; val dy = y2 - y1
        val len = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        if (len < 0.001f) return
        val r = thickness * 0.5f
        val angle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
        push(); translate(x1, y1); rotate(angle)
        addShape(0f, -r, len, thickness, r, r, r, r, 0f, c)
        pop()
    }

    fun text(text: String, x: Float, y: Float, size: Float, color: Int, font: LuminaFont = inter) {
        if (text.isEmpty()) return
        textBatch.add(LuminaBackend.TextEntry(text, x, y, size, applyAlpha(color), font, Matrix3x2f(currentTransform), scissorStack.lastOrNull()))
        drawOrder.add(1)
    }

    fun shadowedText(text: String, x: Float, y: Float, size: Float, color: Int, font: LuminaFont = inter, shadowColor: Int = 0, offsetX: Float = 0f, offsetY: Float = 0f, blur: Float = 0f) {
        if (text.isEmpty()) return
        val scissor = scissorStack.lastOrNull()
        textBatch.add(LuminaBackend.TextEntry(text, x + offsetX, y + offsetY, size, applyAlpha(shadowColor), font, Matrix3x2f(currentTransform), scissor))
        drawOrder.add(1)
        textBatch.add(LuminaBackend.TextEntry(text, x, y, size, applyAlpha(color), font, Matrix3x2f(currentTransform), scissor))
        drawOrder.add(1)
    }

    fun textWidth(text: String, size: Float, font: LuminaFont = inter): Float {
        if (text.isEmpty()) return 0f
        return font.textWidth(text, size)
    }

    fun createImage(path: String): Any = imageCache.getOrPut(path) { loadImage(path) }

    fun createImage(nativeImage: NativeImage): Any {
        val size = nativeImage.width * nativeImage.height * 4
        val copy = MemoryUtil.memAlloc(size)
        copy.put(MemoryUtil.memByteBuffer(nativeImage.pointer, size))
        copy.flip()
        val id = nextWrappedId--
        wrappedTextures[id] = LuminaImage(nativeImage.width, nativeImage.height, rgbaData = copy)
        return id
    }

    fun createImage(width: Int, height: Int, rgbaData: ByteBuffer): Any {
        val copy = MemoryUtil.memAlloc(rgbaData.remaining())
        copy.put(rgbaData.duplicate())
        copy.flip()
        val id = nextWrappedId--
        wrappedTextures[id] = LuminaImage(width, height, rgbaData = copy)
        return id
    }

    fun deleteImage(image: Any?) {
        when (image) {
            is LuminaImage -> {
                val key = imageCache.entries.find { it.value === image }?.key
                if (key != null) imageCache.remove(key)
                image.destroy()
            }
            is Int -> wrappedTextures.remove(image)?.destroy()
        }
    }

    fun image(image: Any?, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f) {
        val tex = resolveTexture(image) ?: return
        imageBatch.add(LuminaBackend.ImageEntry(tex, x, y, w, h, 0f, 0f, 1f, 1f, radius, applyAlpha(0xFFFFFFFF.toInt()), Matrix3x2f(currentTransform), scissorStack.lastOrNull()))
        drawOrder.add(2)
    }

    fun image(image: Any?, x: Float, y: Float, w: Float, h: Float, color: Int) {
        val tex = resolveTexture(image) ?: return
        imageBatch.add(LuminaBackend.ImageEntry(tex, x, y, w, h, 0f, 0f, 1f, 1f, 0f, applyAlpha(color), Matrix3x2f(currentTransform), scissorStack.lastOrNull()))
        drawOrder.add(2)
    }

    fun image(image: Any?, textureWidth: Int, textureHeight: Int, subX: Int, subY: Int, subW: Int, subH: Int, x: Float, y: Float, w: Float, h: Float, radius: Float = 0f) {
        val tex = resolveTexture(image) ?: return
        val u0 = subX.toFloat() / textureWidth; val v0 = subY.toFloat() / textureHeight
        val u1 = (subX + subW).toFloat() / textureWidth; val v1 = (subY + subH).toFloat() / textureHeight
        imageBatch.add(LuminaBackend.ImageEntry(tex, x, y, w, h, u0, v0, u1, v1, radius, applyAlpha(0xFFFFFFFF.toInt()), Matrix3x2f(currentTransform), scissorStack.lastOrNull()))
        drawOrder.add(2)
    }

    private fun resolveTexture(image: Any?): Int? = when (image) {
        is LuminaImage -> { image.ensureUploaded(); image.textureId.takeIf { it != 0 } }
        is String -> { val c = imageCache.getOrPut(image) { loadImage(image) }; c.ensureUploaded(); c.textureId.takeIf { it != 0 } }
        is Int -> wrappedTextures[image]?.let { it.ensureUploaded(); it.textureId.takeIf { id -> id != 0 } }
        else -> null
    }

    private fun loadImage(path: String): LuminaImage {
        val stream = Lumina::class.java.getResourceAsStream(path.trim())
            ?: throw java.io.FileNotFoundException("Image not found: $path")
        if (path.endsWith(".svg", true)) {
            val svgText = stream.bufferedReader().use { it.readText() }
            stream.close()
            return LuminaSvg.loadAndRasterize(svgText, 4f)
        }
        val bytes = stream.use { it.readBytes() }
        val buf = MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as java.nio.ByteBuffer
        val wArr = IntArray(1); val hArr = IntArray(1); val ch = IntArray(1)
        val pixels = STBImage.stbi_load_from_memory(buf, wArr, hArr, ch, 4)
            ?: throw RuntimeException("Failed to load image: $path")
        MemoryUtil.memFree(buf)
        val copy = MemoryUtil.memAlloc(wArr[0] * hArr[0] * 4)
        copy.put(pixels); copy.flip()
        STBImage.stbi_image_free(pixels)
        return LuminaImage(wArr[0], hArr[0], rgbaData = copy)
    }

    fun flush(context: GuiGraphicsExtractor) {
        if (shapeBatch.isEmpty() && textBatch.isEmpty() && imageBatch.isEmpty()) return
        val shapes = ArrayList(shapeBatch); shapeBatch.clear()
        val text = ArrayList(textBatch); textBatch.clear()
        val images = ArrayList(imageBatch); imageBatch.clear()
        val order = ArrayList(drawOrder); drawOrder.clear()

        data class RenderGroup(val isShape: Boolean, val shapeCount: Int, val textCount: Int, val imageCount: Int)
        val groups = mutableListOf<RenderGroup>()
        var i = 0
        while (i < order.size) {
            if (order[i] == 0) {
                var c = 0; while (i < order.size && order[i] == 0) { c++; i++ }
                groups.add(RenderGroup(true, c, 0, 0))
            } else {
                var tc = 0; var ic = 0
                while (i < order.size && order[i] != 0) { if (order[i] == 1) tc++ else ic++; i++ }
                groups.add(RenderGroup(false, 0, tc, ic))
            }
        }

        LuminaPIPRenderer.draw(context, 0, 0, context.guiWidth(), context.guiHeight()) { vw, vh ->
            var si = 0; var ti = 0; var ii = 0
            for (g in groups) {
                if (g.isShape) {
                    val end = (si + g.shapeCount).coerceAtMost(shapes.size)
                    if (end > si) backend.renderShapes(shapes.subList(si, end), vw, vh)
                    si = end
                } else {
                    val te = (ti + g.textCount).coerceAtMost(text.size)
                    val ie = (ii + g.imageCount).coerceAtMost(images.size)
                    if (te > ti || ie > ii) backend.renderTextured(text.subList(ti, te), images.subList(ii, ie), vw, vh)
                    ti = te; ii = ie
                }
            }
        }
    }

    private fun addShape(x: Float, y: Float, w: Float, h: Float, tl: Float, tr: Float, br: Float, bl: Float, border: Float, color: Int, gradType: Int = 0, gradC1: Int = 0, gradC2: Int = 0) {
        shapeBatch.add(QueuedShape(x, y, w, h, tl, tr, br, bl, border, color, gradType, gradC1, gradC2, Matrix3x2f(currentTransform), scissorStack.lastOrNull()))
        drawOrder.add(0)
    }

    private fun applyAlpha(color: Int): Int {
        if (alpha >= 1f) return color
        val a = ((color ushr 24) * alpha).toInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0x00FFFFFF)
    }
}
