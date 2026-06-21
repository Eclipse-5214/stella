package co.stellarskys.stella.api.lumina.renderer

import co.stellarskys.stella.api.lumina.Lumina
import co.stellarskys.stella.api.lumina.types.LuminaFont
import org.joml.Matrix3x2f
import org.joml.Vector2f
import java.nio.ByteBuffer
import kotlin.math.cos
import kotlin.math.sin

interface LuminaBackend {
    enum class TextureFormat { RGBA, R8 }

    fun renderShapes(shapes: List<Lumina.QueuedShape>, vw: Int, vh: Int)
    fun renderTextured(text: List<TextEntry>, images: List<ImageEntry>, vw: Int, vh: Int)
    fun uploadTexture(width: Int, height: Int, data: ByteBuffer, format: TextureFormat, mipmap: Boolean = false): Int
    fun deleteTexture(id: Int)
    fun setupRenderTarget(targetId: Long, width: Int, height: Int)
    fun resetAfterRender()
    fun destroy()

    data class TextEntry(
        val text: String, val x: Float, val y: Float, val size: Float,
        val color: Int, val font: LuminaFont, val transform: Matrix3x2f, val scissor: Lumina.ScissorRect?
    )

    data class ImageEntry(
        val textureId: Int, val x: Float, val y: Float, val w: Float, val h: Float,
        val u0: Float, val v0: Float, val u1: Float, val v1: Float,
        val radius: Float, val color: Int, val transform: Matrix3x2f, val scissor: Lumina.ScissorRect?
    )

    companion object {
        private const val ARC_SEGS = 8
        private const val PI = Math.PI.toFloat()
        private const val HALF_PI = (Math.PI * 0.5).toFloat()

        fun generateOutline(x: Float, y: Float, w: Float, h: Float, tl: Float, tr: Float, br: Float, bl: Float): List<Vector2f> {
            val pts = mutableListOf<Vector2f>()
            var f = 1f
            if (tl + tr > 0f) f = minOf(f, w / (tl + tr))
            if (tr + br > 0f) f = minOf(f, h / (tr + br))
            if (br + bl > 0f) f = minOf(f, w / (br + bl))
            if (bl + tl > 0f) f = minOf(f, h / (bl + tl))
            val rtl = tl * f; val rtr = tr * f; val rbr = br * f; val rbl = bl * f
            pts.add(Vector2f(x + rtl, y)); pts.add(Vector2f(x + w - rtr, y))
            arcPts(pts, x + w - rtr, y + rtr, rtr, -HALF_PI, 0f)
            pts.add(Vector2f(x + w, y + rtr)); pts.add(Vector2f(x + w, y + h - rbr))
            arcPts(pts, x + w - rbr, y + h - rbr, rbr, 0f, HALF_PI)
            pts.add(Vector2f(x + w - rbr, y + h)); pts.add(Vector2f(x + rbl, y + h))
            arcPts(pts, x + rbl, y + h - rbl, rbl, HALF_PI, PI)
            pts.add(Vector2f(x, y + h - rbl)); pts.add(Vector2f(x, y + rtl))
            arcPts(pts, x + rtl, y + rtl, rtl, PI, PI + HALF_PI)
            val out = mutableListOf<Vector2f>()
            for (p in pts) if (out.lastOrNull()?.let { distSq(p, it) > 1e-6f } != false) out.add(p)
            if (out.size > 1 && distSq(out.first(), out.last()) < 1e-6f) out.removeAt(out.size - 1)
            return out
        }

        private fun arcPts(out: MutableList<Vector2f>, cx: Float, cy: Float, r: Float, a0: Float, a1: Float) {
            if (r < 0.001f) return
            for (i in 1..ARC_SEGS) {
                val a = a0 + (a1 - a0) * i / ARC_SEGS
                out.add(Vector2f(cx + cos(a) * r, cy + sin(a) * r))
            }
        }

        private fun distSq(a: Vector2f, b: Vector2f) = (a.x - b.x) * (a.x - b.x) + (a.y - b.y) * (a.y - b.y)
    }
}
