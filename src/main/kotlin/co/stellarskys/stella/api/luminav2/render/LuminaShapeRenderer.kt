package co.stellarskys.stella.api.luminav2.render

import co.stellarskys.stella.api.luminav2.render.LuminaPipelines
import co.stellarskys.stella.api.luminav2.LuminaV2
import co.stellarskys.stella.api.luminav2.LuminaV2.Mask
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import org.joml.Matrix3x2f
import org.joml.Vector2f
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object LuminaShapeRenderer {
    private val FORMAT = DefaultVertexFormat.POSITION_COLOR
    private val VERTEX_STRIDE = FORMAT.vertexSize

    private const val VERTICES_PER_FAN = 3
    private const val VERTICES_PER_FRINGE = 6
    private const val VERTICES_PER_RING = 6

    private const val PI = Math.PI.toFloat()
    private const val HALF_PI = PI / 2f
    private const val SMALL = 0.001f
    private const val TINY = 1e-6f // insert falling cat

    private const val ARC_SEGMENTS = 8
    private const val FRINGE_WIDTH = 1f

    class ShapeEntry(
        val x: Float, val y: Float, val w: Float, val h: Float,
        val tl: Float, val tr: Float, val br: Float, val bl: Float, // Radii
        val border: Float, val color: Int,
        transform: Matrix3x2f, scissor: LuminaV2.ScissorRect?,
        val gradient: LuminaV2.Gradient? = null
    ): LuminaV2.Entry(transform, scissor) {
        val outlinePoints by lazy (LazyThreadSafetyMode.NONE) { getOutline(x, y, w, h, outlineClamps) }
        val outlineClamps by lazy (LazyThreadSafetyMode.NONE) { clampRadii(tl, tr, br, bl, w, h) }

        val innerClamps by lazy (LazyThreadSafetyMode.NONE) {
            FloatArray(4) { i -> maxOf(0f, outlineClamps[i] - border) }
        }

        val innerPoints by lazy (LazyThreadSafetyMode.NONE) {
            getOutline(
                x + border, y + border,
                w - 2f * border, h - 2f * border,
                innerClamps, outlineClamps
            )
        }

        val outlineNormalPoints by lazy (LazyThreadSafetyMode.NONE) { getNormals(outlinePoints) }
        val innerNormalPoints by lazy (LazyThreadSafetyMode.NONE) { getNormals(innerPoints) }

        val cx by lazy (LazyThreadSafetyMode.NONE) { x + w / 2f }
        val cy by lazy (LazyThreadSafetyMode.NONE) { y + h / 2f }

        val color1 = gradient?.color1 ?: color
        val color2 = gradient?.color2 ?: color
    }

    private data class Corner(val x: Float, val y: Float, val radius: Float, val outer: Float?, val startAngle: Float)

    fun drawShapes(device: GpuDevice, pass: RenderPass, shapes: List<ShapeEntry>) {
        if (shapes.isEmpty()) return

        val dpr = LuminaV2.dpr
        val vertices = shapes.sumOf {
            if(it.border > 0f) it.outlinePoints.size * (VERTICES_PER_RING + VERTICES_PER_FRINGE * 2)
            else it.outlinePoints.size * (VERTICES_PER_FAN + VERTICES_PER_FRINGE)
        }

        ByteBufferBuilder(vertices * VERTEX_STRIDE).use { bb ->
            BufferBuilder(bb, PrimitiveTopology.TRIANGLES, FORMAT).apply {
                for (shape in shapes) {
                    if (shape.border > 0f) {
                        ring(shape, dpr, shape.outlinePoints, shape.innerPoints, shape.outlineNormalPoints, shape.innerNormalPoints)
                        fringe(shape, dpr, shape.outlinePoints, shape.outlineNormalPoints)
                        fringe(shape, dpr, shape.innerPoints, shape.innerNormalPoints, -1f)
                    } else {
                        fan(shape, dpr, shape.cx, shape.cy, shape.outlinePoints, shape.outlineNormalPoints)
                        fringe(shape, dpr, shape.outlinePoints, shape.outlineNormalPoints)
                    }
                }
            }.buildOrThrow().use { mesh ->
                device.createBuffer({ "lumina shapes" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { buffer ->
                    pass.setPipeline(getPipeline(shapes))
                    pass.setVertexBuffer(0, buffer.slice())
                    pass.draw(vertices, 1, 0, 0)
                }
            }
        }
    }

    private fun getPipeline(batch: List<LuminaV2.Entry>) = when(batch.first().mask) {
        Mask.Write -> LuminaPipelines.SHAPE_WRITE
        Mask.In -> LuminaPipelines.SHAPE_IN
        Mask.None -> LuminaPipelines.SHAPE
    }

    fun getOutline(x: Float, y: Float, w: Float, h: Float, clampedRadii: FloatArray, outerClamps: FloatArray? = null): List<Vector2f> {
        val (radiusTl, radiusTr, radiusBr, radiusBl) = clampedRadii

        val corners = listOf(
            Corner(x + radiusTl, y + radiusTl, radiusTl, outerClamps?.getOrNull(0), PI, ), // 180
            Corner(x + w - radiusTr, y + radiusTr, radiusTr, outerClamps?.getOrNull(1), PI + HALF_PI), // 270
            Corner(x + w - radiusBr, y + h - radiusBr, radiusBr, outerClamps?.getOrNull(2), 0f), // 0 duh
            Corner(x + radiusBl, y + h - radiusBl, radiusBl, outerClamps?.getOrNull(3), HALF_PI), // 90
        )

        val points = mutableListOf<Vector2f>()
        for (corner in corners) {
            if (corner.radius < SMALL && (corner.outer == null || corner.outer < SMALL)) points.add(
                Vector2f(
                    corner.x,
                    corner.y
                )
            )
            else for (i in 0..ARC_SEGMENTS) {
                val angle = corner.startAngle + HALF_PI * i / ARC_SEGMENTS
                points.add(
                    Vector2f(
                        corner.x + cos(angle) * corner.radius,
                        corner.y + sin(angle) * corner.radius
                    )
                )
            }
        }
        return points
    }

    fun clampRadii(tl: Float, tr: Float, br: Float, bl: Float, w: Float, h: Float): FloatArray {
        val cFactor = 1f
            .clampEdge(tl, tr, w).clampEdge(br, bl, w)
            .clampEdge(bl, tl, h).clampEdge(tr, br, h)
        return floatArrayOf(tl * cFactor, tr * cFactor, br * cFactor, bl * cFactor)
    }

    private fun Float.clampEdge(c1: Float, c2: Float, edge: Float): Float {
        val sum = c1 + c2
        if (sum <= 0) return this
        return minOf(this, edge / sum)
    }

    fun getNormals(points: List<Vector2f>): List<Vector2f> = List(points.size) { index ->
        val size = points.size
        val prev = points[(index - 1 + size) % size]
        val current = points[index]
        val next = points[(index + 1) % size]

        val prevNormal = perpendicular(current.x - prev.x, current.y - prev.y)
        val nextNormal = perpendicular(next.x - current.x, next.y - current.y)

        val average = Vector2f(prevNormal.x + nextNormal.x, prevNormal.y + nextNormal.y)
        val length = average.length()
        if (length > TINY) average.normalize() else average
    }

    private fun perpendicular(dx: Float, dy: Float): Vector2f {
        val length = sqrt(dx * dx + dy * dy) // shoutout distance formula
        if (length < TINY) return Vector2f(0f, 0f)
        return Vector2f(dy / length, -dx / length)
    }

    private fun fringeWidth(entry: LuminaV2.Entry, dpr: Float): Float {
        val scale = sqrt(entry.transform.m00 * entry.transform.m00 + entry.transform.m01 * entry.transform.m01)
        return (FRINGE_WIDTH / dpr) / if (scale > TINY) scale else 1f
    }

    private fun colorAt(point: Vector2f, shape: ShapeEntry): Int = lerpColor(shape.color1, shape.color2, gradientFrac(point, shape))

    private fun lerpColor(color1: Int, color2: Int, fraction: Float): Int {
        val newFrac = fraction.coerceIn(0f, 1f)
        val a = color1.a.lerp(color2.a, newFrac).roundToInt()
        val r = color1.r.lerp(color2.r, newFrac).roundToInt()
        val g = color1.g.lerp(color2.g, newFrac).roundToInt()
        val b = color1.b.lerp(color2.b, newFrac).roundToInt()
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    private fun gradientFrac(point: Vector2f, shape: ShapeEntry): Float {
        val type = shape.gradient?.type ?: return 0f
        val u = ((point.x - shape.x) / shape.w).coerceIn(0f, 1f)
        val v = ((point.y - shape.y) / shape.h).coerceIn(0f, 1f)
        return when (type) {
            LuminaV2.Gradient.Type.LeftToRight -> u
            LuminaV2.Gradient.Type.TopToBottom -> v
            LuminaV2.Gradient.Type.TopLeftToBottomRight -> (u + v) / 2f
            LuminaV2.Gradient.Type.BottomRightToTopLeft -> 1f - (u + v) / 2f
        }
    }

    private fun BufferBuilder.fan(entry: ShapeEntry, dpr: Float, centerX: Float, centerY: Float, points: List<Vector2f>, normals: List<Vector2f>) {
        val halfFW = fringeWidth(entry, dpr) / 2f
        val center = entry.toScreen(centerX, centerY, dpr)
        val centerColor = colorAt(Vector2f(centerX, centerY), entry)
        val size = points.size

        for(index in 0 until size) {
            val nextIndex = (index + 1) % size
            val local1 = points[index]
            val local2 = points[nextIndex]
            val point1 = local1.inset(normals[index], halfFW).toScreen(entry, dpr)
            val point2 = local2.inset(normals[nextIndex], halfFW).toScreen(entry, dpr)

            addVertex(center.x, center.y, 0f).setColor(centerColor)
            addVertex(point1.x, point1.y, 0f).setColor(colorAt(local1, entry))
            addVertex(point2.x, point2.y, 0f).setColor(colorAt(local2, entry))
        }
    }

    private fun BufferBuilder.ring(entry: ShapeEntry, dpr: Float, outer: List<Vector2f>, inner: List<Vector2f>, outerNormals: List<Vector2f>, innerNormals: List<Vector2f>) {
        val halfFW = minOf(fringeWidth(entry, dpr), entry.border) / 2f
        val size = outer.size

        for(index in 0 until size) {
            val nextIndex = (index + 1) % size
            val colorNow = colorAt(outer[index], entry)
            val colorNext = colorAt(outer[nextIndex], entry)

            val outerNow = outer[index].inset(outerNormals[index], halfFW).toScreen(entry, dpr)
            val innerNow = inner[index].inset(innerNormals[index], -halfFW).toScreen(entry, dpr)
            val outerNext = outer[nextIndex].inset(outerNormals[nextIndex], halfFW).toScreen(entry, dpr)
            val innerNext = inner[nextIndex].inset(innerNormals[nextIndex], -halfFW).toScreen(entry, dpr)

            addVertex(outerNow.x, outerNow.y, 0f).setColor(colorNow)
            addVertex(innerNow.x, innerNow.y, 0f).setColor(colorNow)
            addVertex(innerNext.x, innerNext.y, 0f).setColor(colorNext)

            addVertex(innerNext.x, innerNext.y, 0f).setColor(colorNext)
            addVertex(outerNext.x, outerNext.y, 0f).setColor(colorNext)
            addVertex(outerNow.x, outerNow.y, 0f).setColor(colorNow)
        }
    }

    private fun BufferBuilder.fringe(entry: ShapeEntry, dpr: Float, points: List<Vector2f>, normals: List<Vector2f>, direction: Float = 1f) {
        val halfFW = fringeWidth(entry, dpr) / 2f
        val unclampedFW = fringeWidth(entry, dpr) / 2f * direction
        val clampedFw = if (entry.border > 0) minOf(halfFW, entry.border) * direction else unclampedFW
        val size = points.size

        for(index in 0 until size) {
            val nextIndex = (index + 1) % size
            val colorNow = colorAt(points[index], entry)
            val colorNext = colorAt(points[nextIndex], entry)
            val transparentNow = colorNow.alpha0
            val transparentNext = colorNext.alpha0

            val innerNow = points[index].inset(normals[index], clampedFw).toScreen(entry, dpr)
            val outerNow = points[index].inset(normals[index], -unclampedFW).toScreen(entry, dpr)
            val innerNext = points[nextIndex].inset(normals[nextIndex], clampedFw).toScreen(entry, dpr)
            val outerNext = points[nextIndex].inset(normals[nextIndex], -unclampedFW).toScreen(entry, dpr)

            addVertex(innerNow.x, innerNow.y, 0f).setColor(colorNow)
            addVertex(outerNow.x, outerNow.y, 0f).setColor(transparentNow)
            addVertex(innerNext.x, innerNext.y, 0f).setColor(colorNext)

            addVertex(innerNext.x, innerNext.y, 0f).setColor(colorNext)
            addVertex(outerNow.x, outerNow.y, 0f).setColor(transparentNow)
            addVertex(outerNext.x, outerNext.y, 0f).setColor(transparentNext)
        }
    }

    private inline val Int.a get() = (this ushr 24) and 0xFF
    private inline val Int.r get() = (this ushr 16) and 0xFF
    private inline val Int.g get() = (this ushr 8) and 0xFF
    private inline val Int.b get() = this and 0xFF
    private inline val Int.alpha0 get() = this and 0x00FFFFFF // Color(255, 255, 255, 0)

    private fun Int.lerp(int2: Int, frac: Float): Float = this + (int2 - this) * frac

    private fun Vector2f.toScreen(entry: LuminaV2.Entry, dpr: Float): Vector2f = entry.toScreen(x, y, dpr)
    private fun Vector2f.inset(normal: Vector2f, amount: Float): Vector2f =
        Vector2f(x - normal.x * amount, y - normal.y * amount)
}