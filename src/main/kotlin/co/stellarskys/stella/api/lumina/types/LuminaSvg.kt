package co.stellarskys.stella.api.lumina.types

/*
 * Edge-based rasterizer architecture adapted from NanoSVG by Mikko Mononen.
 * Built with the assistance of Claude (Anthropic).
 *
 * NanoSVG: https://github.com/memononen/nanosvg (zlib license)
 */

import org.lwjgl.system.MemoryUtil
import java.nio.ByteBuffer
import kotlin.math.*

object LuminaSvg {
    private val viewBoxRegex = """viewBox\s*=\s*"([^"]+)"""".toRegex()
    private val widthRegex = """width\s*=\s*"(\d+)"""".toRegex()
    private val heightRegex = """height\s*=\s*"(\d+)"""".toRegex()
    private val gTransformRegex = """<g\s[^>]*transform\s*=\s*"([^"]+)"[^>]*>""".toRegex()
    private val translateRegex = """translate\(([^)]+)\)""".toRegex()
    private val scaleRegex = """scale\(([^)]+)\)""".toRegex()
    private val gFillRegex = """<g\s[^>]*fill\s*=\s*"([^"]+)"""".toRegex()
    private val gStrokeRegex = """<g\s[^>]*stroke\s*=\s*"([^"]+)"""".toRegex()
    private val pathRegex = """<path\s[^>]*>""".toRegex(RegexOption.DOT_MATCHES_ALL)
    private val whitespaceRegex = """\s+""".toRegex()

    private fun String.attr(name: String) = """$name\s*=\s*"([^"]+)"""".toRegex().find(this)?.groupValues?.get(1)

    fun loadAndRasterize(svgText: String, scale: Float = 4f): LuminaImage {
        val svg = parseSvg(svgText)
        val w = (svg.width * scale).toInt().coerceAtLeast(1)
        val h = (svg.height * scale).toInt().coerceAtLeast(1)
        val bitmap = MemoryUtil.memCalloc(w * h * 4)
        rasterize(svg, bitmap, w, h, scale)
        return LuminaImage(w, h, rgbaData = bitmap)
    }

    private data class Svg(val width: Float, val height: Float, val shapes: List<SvgShape>)

    private data class SvgShape(
        val points: List<Float>, val closed: Boolean,
        val fill: IntArray?, val stroke: IntArray?,
        val strokeWidth: Float, val roundCap: Boolean, val roundJoin: Boolean
    )

    private data class Edge(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val dir: Int)

    private fun parseSvg(text: String): Svg {
        var vbW = widthRegex.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 24f
        var vbH = heightRegex.find(text)?.groupValues?.get(1)?.toFloatOrNull() ?: 24f
        viewBoxRegex.find(text)?.let {
            val parts = it.groupValues[1].trim().split(whitespaceRegex)
            if (parts.size >= 4) { vbW = parts[2].toFloatOrNull() ?: vbW; vbH = parts[3].toFloatOrNull() ?: vbH }
        }

        var gtx = 0f; var gty = 0f; var gsx = 1f; var gsy = 1f
        gTransformRegex.find(text)?.let { m ->
            val tr = m.groupValues[1]
            translateRegex.find(tr)?.let { val p = it.groupValues[1].split(",").map { s -> s.trim().toFloatOrNull() ?: 0f }; gtx = p.getOrElse(0) { 0f }; gty = p.getOrElse(1) { 0f } }
            scaleRegex.find(tr)?.let { val p = it.groupValues[1].split(",").map { s -> s.trim().toFloatOrNull() ?: 1f }; gsx = p.getOrElse(0) { 1f }; gsy = p.getOrElse(1) { gsx } }
        }

        val gFill = gFillRegex.find(text)?.groupValues?.get(1)
        val gStroke = gStrokeRegex.find(text)?.groupValues?.get(1)

        val shapes = mutableListOf<SvgShape>()
        for (match in pathRegex.findAll(text)) {
            val tag = match.value
            val d = tag.attr("d") ?: continue
            val fillStr = tag.attr("fill") ?: gFill ?: "none"
            val strokeStr = tag.attr("stroke") ?: gStroke ?: "none"
            val strokeW = tag.attr("stroke-width")?.toFloatOrNull() ?: 1f

            val rawPts = parsePath(d)
            val pts = if (gsx != 1f || gsy != 1f || gtx != 0f || gty != 0f)
                rawPts.chunked(2).flatMap { (x, y) -> listOf(x * gsx + gtx, y * gsy + gty) }
            else rawPts

            shapes.add(SvgShape(pts, d.contains('Z', true),
                if (fillStr != "none") parseColor(fillStr) else null,
                if (strokeStr != "none") parseColor(strokeStr) else null,
                strokeW * abs(gsx), tag.attr("stroke-linecap") == "round", tag.attr("stroke-linejoin") == "round"))
        }
        return Svg(vbW, vbH, shapes)
    }

    private fun parseColor(color: String): IntArray {
        if (color.startsWith("#") && color.length == 7)
            return intArrayOf(color.substring(1, 3).toInt(16), color.substring(3, 5).toInt(16), color.substring(5, 7).toInt(16), 255)
        return intArrayOf(255, 255, 255, 255)
    }

    private fun parsePath(d: String): List<Float> {
        val pts = mutableListOf<Float>()
        var cx = 0f; var cy = 0f; var sx = 0f; var sy = 0f
        val tokens = tokenize(d)
        var i = 0; var lastCmd = ' '

        while (i < tokens.size) {
            val tok = tokens[i]
            val cmd = if (tok.length == 1 && tok[0].isLetter()) { i++; tok[0] } else lastCmd
            when (cmd) {
                'M' -> { cx = tokens[i++].toFloat(); cy = tokens[i++].toFloat(); pts.add(cx); pts.add(cy); sx = cx; sy = cy; lastCmd = 'L' }
                'm' -> { cx += tokens[i++].toFloat(); cy += tokens[i++].toFloat(); pts.add(cx); pts.add(cy); sx = cx; sy = cy; lastCmd = 'l' }
                'L' -> { cx = tokens[i++].toFloat(); cy = tokens[i++].toFloat(); pts.add(cx); pts.add(cy); lastCmd = 'L' }
                'l' -> { cx += tokens[i++].toFloat(); cy += tokens[i++].toFloat(); pts.add(cx); pts.add(cy); lastCmd = 'l' }
                'H' -> { cx = tokens[i++].toFloat(); pts.add(cx); pts.add(cy); lastCmd = 'H' }
                'h' -> { cx += tokens[i++].toFloat(); pts.add(cx); pts.add(cy); lastCmd = 'h' }
                'V' -> { cy = tokens[i++].toFloat(); pts.add(cx); pts.add(cy); lastCmd = 'V' }
                'v' -> { cy += tokens[i++].toFloat(); pts.add(cx); pts.add(cy); lastCmd = 'v' }
                'C' -> { val x1 = tokens[i++].toFloat(); val y1 = tokens[i++].toFloat(); val x2 = tokens[i++].toFloat(); val y2 = tokens[i++].toFloat(); val x3 = tokens[i++].toFloat(); val y3 = tokens[i++].toFloat(); flattenCubic(pts, cx, cy, x1, y1, x2, y2, x3, y3); cx = x3; cy = y3; lastCmd = 'C' }
                'c' -> { val x1 = cx + tokens[i++].toFloat(); val y1 = cy + tokens[i++].toFloat(); val x2 = cx + tokens[i++].toFloat(); val y2 = cy + tokens[i++].toFloat(); val x3 = cx + tokens[i++].toFloat(); val y3 = cy + tokens[i++].toFloat(); flattenCubic(pts, cx, cy, x1, y1, x2, y2, x3, y3); cx = x3; cy = y3; lastCmd = 'c' }
                'Z', 'z' -> { cx = sx; cy = sy; lastCmd = cmd }
                else -> { i++ }
            }
        }
        return pts
    }

    private fun flattenCubic(out: MutableList<Float>, x0: Float, y0: Float, x1: Float, y1: Float, x2: Float, y2: Float, x3: Float, y3: Float, depth: Int = 0) {
        if (depth > 8) { out.add(x3); out.add(y3); return }
        val dx = x3 - x0; val dy = y3 - y0
        val d2 = abs((x1 - x3) * dy - (y1 - y3) * dx); val d3 = abs((x2 - x3) * dy - (y2 - y3) * dx)
        if ((d2 + d3) * (d2 + d3) < 0.25f * (dx * dx + dy * dy)) { out.add(x3); out.add(y3); return }
        val x01 = (x0 + x1) * 0.5f; val y01 = (y0 + y1) * 0.5f; val x12 = (x1 + x2) * 0.5f; val y12 = (y1 + y2) * 0.5f
        val x23 = (x2 + x3) * 0.5f; val y23 = (y2 + y3) * 0.5f; val x012 = (x01 + x12) * 0.5f; val y012 = (y01 + y12) * 0.5f
        val x123 = (x12 + x23) * 0.5f; val y123 = (y12 + y23) * 0.5f; val x0123 = (x012 + x123) * 0.5f; val y0123 = (y012 + y123) * 0.5f
        flattenCubic(out, x0, y0, x01, y01, x012, y012, x0123, y0123, depth + 1)
        flattenCubic(out, x0123, y0123, x123, y123, x23, y23, x3, y3, depth + 1)
    }

    private fun tokenize(d: String): List<String> {
        val tokens = mutableListOf<String>(); val sb = StringBuilder()
        for (c in d) {
            if (c.isLetter()) { if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() }; tokens.add(c.toString()) }
            else if (c == ',' || c == ' ' || c == '\n' || c == '\r' || c == '\t') { if (sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear() } }
            else if (c == '-' && sb.isNotEmpty()) { tokens.add(sb.toString()); sb.clear(); sb.append(c) }
            else sb.append(c)
        }
        if (sb.isNotEmpty()) tokens.add(sb.toString())
        return tokens
    }

    // Stroke expansion (NanoSVG-style edge-based architecture)

    private val edges = mutableListOf<Edge>()

    private fun addEdge(x0: Float, y0: Float, x1: Float, y1: Float) {
        if (y0 == y1) return
        if (y0 < y1) edges.add(Edge(x0, y0, x1, y1, 1)) else edges.add(Edge(x1, y1, x0, y0, -1))
    }

    private fun expandStroke(pts: List<Float>, strokeWidth: Float, closed: Boolean, roundCap: Boolean, scale: Float) {
        val n = pts.size / 2; if (n < 2) return
        val w = strokeWidth * scale * 0.5f
        val ncap = maxOf(4, (PI * w / 2).toInt())

        val dx = FloatArray(n); val dy = FloatArray(n)
        for (i in 0 until n - 1) {
            var ddx = pts[(i + 1) * 2] * scale - pts[i * 2] * scale; var ddy = pts[(i + 1) * 2 + 1] * scale - pts[i * 2 + 1] * scale
            val len = sqrt(ddx * ddx + ddy * ddy); if (len > 1e-6f) { ddx /= len; ddy /= len }
            dx[i] = ddx; dy[i] = ddy
        }
        if (closed) { dx[n - 1] = dx[0]; dy[n - 1] = dy[0] } else { dx[n - 1] = dx[n - 2]; dy[n - 1] = dy[n - 2] }

        var lx = 0f; var ly = 0f; var rx = 0f; var ry = 0f
        var firstLx = 0f; var firstLy = 0f; var firstRx = 0f; var firstRy = 0f
        val s: Int; val e: Int

        if (closed) {
            val dlx = dy[n - 1]; val dly = -dx[n - 1]
            lx = pts[0] * scale - dlx * w; ly = pts[1] * scale - dly * w
            rx = pts[0] * scale + dlx * w; ry = pts[1] * scale + dly * w
            firstLx = lx; firstLy = ly; firstRx = rx; firstRy = ry; s = 0; e = n
        } else {
            val px = pts[0] * scale; val py = pts[1] * scale
            if (roundCap) {
                roundCap(px, py, dx[0], dy[0], w, ncap, false, lx, ly, rx, ry).let { lx = it[0]; ly = it[1]; rx = it[2]; ry = it[3] }
            } else {
                val dlx = dy[0]; val dly = -dx[0]
                lx = px - dlx * w; ly = py - dly * w; rx = px + dlx * w; ry = py + dly * w
                addEdge(lx, ly, rx, ry)
            }
            s = 1; e = n - 1
        }

        for (j in s until e) {
            val prevI = if (j > 0) j - 1 else n - 1
            val px = pts[j * 2] * scale; val py = pts[j * 2 + 1] * scale
            val a0 = atan2(-dx[prevI], dy[prevI]); val a1 = atan2(-dx[j], dy[j])
            var da = a1 - a0; if (da < -PI) da += 2 * PI.toFloat(); if (da > PI) da -= 2 * PI.toFloat()
            val nn = maxOf(2, minOf(ncap, ceil(abs(da) / PI.toFloat() * ncap).toInt()))

            var nlx = lx; var nly = ly; var nrx = rx; var nry = ry
            for (i in 0 until nn) {
                val a = a0 + i.toFloat() / (nn - 1).toFloat() * da
                val ax = cos(a) * w; val ay = sin(a) * w
                val lx1 = px - ax; val ly1 = py - ay; val rx1 = px + ax; val ry1 = py + ay
                addEdge(lx1, ly1, nlx, nly); addEdge(nrx, nry, rx1, ry1)
                nlx = lx1; nly = ly1; nrx = rx1; nry = ry1
            }
            lx = nlx; ly = nly; rx = nrx; ry = nry
        }

        if (closed) { addEdge(firstLx, firstLy, lx, ly); addEdge(rx, ry, firstRx, firstRy) }
        else {
            val lastI = n - 1; val px = pts[lastI * 2] * scale; val py = pts[lastI * 2 + 1] * scale
            if (roundCap) { roundCap(px, py, -dx[lastI - 1], -dy[lastI - 1], w, ncap, true, rx, ry, lx, ly) }
            else {
                val dlx = dy[lastI - 1]; val dly = -dx[lastI - 1]
                val elx = px - dlx * w; val ely = py - dly * w; val erx = px + dlx * w; val ery = py + dly * w
                addEdge(erx, ery, rx, ry); addEdge(erx, ery, elx, ely); addEdge(lx, ly, elx, ely)
            }
        }
    }

    private fun roundCap(px: Float, py: Float, ddx: Float, ddy: Float, w: Float, ncap: Int, connect: Boolean, prevLx: Float, prevLy: Float, prevRx: Float, prevRy: Float): FloatArray {
        val dlx = ddy; val dly = -ddx
        var capLx = 0f; var capLy = 0f; var capRx = 0f; var capRy = 0f; var prevX = 0f; var prevY = 0f
        for (i in 0 until ncap) {
            val a = i.toFloat() / (ncap - 1).toFloat() * PI.toFloat()
            val x = px - dlx * cos(a) * w - ddx * sin(a) * w; val y = py - dly * cos(a) * w - ddy * sin(a) * w
            if (i > 0) addEdge(prevX, prevY, x, y)
            prevX = x; prevY = y
            if (i == 0) { capLx = x; capLy = y }; if (i == ncap - 1) { capRx = x; capRy = y }
        }
        if (connect) { addEdge(prevLx, prevLy, capLx, capLy); addEdge(capRx, capRy, prevRx, prevRy) }
        return floatArrayOf(capLx, capLy, capRx, capRy)
    }

    // Scanline rasterizer (non-zero winding, subsampled AA)

    private const val SUBSAMPLES = 5

    private fun rasterize(svg: Svg, bitmap: ByteBuffer, w: Int, h: Int, scale: Float) {
        for (shape in svg.shapes) {
            if (shape.fill != null) {
                edges.clear()
                val n = shape.points.size / 2
                for (i in 0 until n) { val j = (i + 1) % n; addEdge(shape.points[i * 2] * scale, shape.points[i * 2 + 1] * scale, shape.points[j * 2] * scale, shape.points[j * 2 + 1] * scale) }
                rasterizeEdges(bitmap, w, h, shape.fill)
            }
            if (shape.stroke != null && shape.strokeWidth > 0.01f) {
                edges.clear()
                expandStroke(shape.points, shape.strokeWidth, shape.closed, shape.roundCap, scale)
                rasterizeEdges(bitmap, w, h, shape.stroke)
            }
        }
    }

    private fun rasterizeEdges(bitmap: ByteBuffer, w: Int, h: Int, color: IntArray) {
        if (edges.isEmpty()) return
        val sortedEdges = edges.sortedBy { it.y0 }; val scanBuf = IntArray(w)
        val cr = color[0]; val cg = color[1]; val cb = color[2]; val ca = color[3]

        for (py in 0 until h) {
            scanBuf.fill(0)
            for (sub in 0 until SUBSAMPLES) {
                val sy = py.toFloat() + (sub.toFloat() + 0.5f) / SUBSAMPLES
                for (edge in sortedEdges) {
                    if (edge.y0 > sy) break; if (edge.y1 <= sy) continue
                    scanBuf[(edge.x0 + (sy - edge.y0) / (edge.y1 - edge.y0) * (edge.x1 - edge.x0)).toInt().coerceIn(0, w - 1)] += edge.dir
                }
            }
            var winding = 0
            for (px in 0 until w) {
                winding += scanBuf[px]
                val cover = minOf(abs(winding).toFloat() / SUBSAMPLES, 1f); if (cover < 1f / 255f) continue
                val idx = (py * w + px) * 4; val a = (ca * cover).toInt().coerceIn(0, 255); val ia = 255 - a
                bitmap.put(idx, ((cr * a + (bitmap.get(idx).toInt() and 0xFF) * ia) / 255).coerceIn(0, 255).toByte())
                bitmap.put(idx + 1, ((cg * a + (bitmap.get(idx + 1).toInt() and 0xFF) * ia) / 255).coerceIn(0, 255).toByte())
                bitmap.put(idx + 2, ((cb * a + (bitmap.get(idx + 2).toInt() and 0xFF) * ia) / 255).coerceIn(0, 255).toByte())
                bitmap.put(idx + 3, (a + ((bitmap.get(idx + 3).toInt() and 0xFF) * ia) / 255).coerceIn(0, 255).toByte())
            }
        }
    }
}
