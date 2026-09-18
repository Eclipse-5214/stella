package co.stellarskys.stella.api.luminav2.render

import co.stellarskys.stella.api.luminav2.LuminaV2
import co.stellarskys.stella.api.luminav2.LuminaV2.Mask
import co.stellarskys.stella.api.luminav2.render.LuminaPipelines.usePipeline
import co.stellarskys.stella.api.luminav2.types.LuminaFont
import co.stellarskys.stella.api.luminav2.types.LuminaImage
import com.mojang.blaze3d.buffers.GpuBuffer
import com.mojang.blaze3d.systems.GpuDevice
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.BufferBuilder
import com.mojang.blaze3d.vertex.ByteBufferBuilder
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import org.joml.Matrix3x2f

//? if > 26.1 {
 /*import com.mojang.blaze3d.PrimitiveTopology
*///? } else {
import com.mojang.blaze3d.vertex.VertexFormat
//? }


object LuminaTextureRenderer {
    private val FORMAT = DefaultVertexFormat.POSITION_TEX_COLOR
    private val VERTEX_STRIDE = FORMAT.vertexSize

    private const val VERTICIES_PER_QUAD = 6

    class TextEntry(
        val text: String, val x: Float, val y: Float, val size: Float,
        val color: Int, val font: LuminaFont,
        transform: Matrix3x2f, scissor: LuminaV2.ScissorRect?
    ): LuminaV2.Entry(transform, scissor)

    class ImageEntry(
        val image: LuminaImage, val x: Float, val y: Float, val w: Float, val h: Float,
        val u0: Float, val v0: Float, val u1: Float, val v1: Float,
        val radius: Float, val color: Int,
        transform: Matrix3x2f, scissor: LuminaV2.ScissorRect?
    ): LuminaV2.Entry(transform, scissor)

    fun drawImages(device: GpuDevice, pass: RenderPass, images: List<ImageEntry>) {
        if (images.isEmpty()) return

        val dpr = LuminaV2.dpr

        for (entry in images) {
            val sampler = RenderSystem.getSamplerCache().getClampToEdge(entry.image.filterMode)

            ByteBufferBuilder(VERTICIES_PER_QUAD * VERTEX_STRIDE).use { bb ->
                BufferBuilder(bb, /*? if > 26.1 {*//*PrimitiveTopology.TRIANGLES*//*?} else {*/ VertexFormat.Mode.TRIANGLES /*?}*/, FORMAT).apply {
                    val x0 = entry.x;
                    val y0 = entry.y;
                    val x1 = entry.x + entry.w;
                    val y1 = entry.y + entry.h;

                    quad(entry, dpr, x0, y0, x1, y1, entry.u0, entry.v0, entry.u1, entry.v1, entry.color)
                }.buildOrThrow().use { mesh ->
                    device.createBuffer({ "lumina image" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { buffer ->
                        val texView = entry.image.textureView ?: return@use

                        pass.usePipeline(getPipeline(images))
                        pass.setVertexBuffer(0, buffer /*? if > 26.1 {*//*.slice()*//*?}*/)
                        /*? if >= 26.3 {*/ /*pass.setUniform("Sampler0", texView, sampler) *//*?} else {*/ pass.bindTexture("Sampler0", texView, sampler) /*?}*/
                        pass.draw(/*? if < 26.2 { */ 0, VERTICIES_PER_QUAD /*? } else { *//*VERTICIES_PER_QUAD, 1, 0, 0 *//*? } */)
                    }
                }
            }
        }
    }

    fun drawText(device: GpuDevice, pass: RenderPass, texts: List<TextEntry>) {
        if (texts.isEmpty()) return

        val dpr = LuminaV2.dpr

        for (entry in texts) {
            val font = entry.font
            val scale = entry.size / font.config.bakeSize

            val xCursor = floatArrayOf(0f)
            val yCursor = floatArrayOf(font.alignTopPx)

            val quads = entry.text.map { entry.font.getGlyphQuad(it, xCursor, yCursor) }.filterNotNull()
            val vertices = VERTICIES_PER_QUAD * quads.size

            val sampler = RenderSystem.getSamplerCache().getClampToEdge(font.atlas.filterMode)

            ByteBufferBuilder(VERTEX_STRIDE * vertices).use { bb ->
                BufferBuilder(bb, /*? if > 26.1 {*//*PrimitiveTopology.TRIANGLES*//*?} else {*/ VertexFormat.Mode.TRIANGLES /*?}*/, FORMAT).apply {
                for (quad in quads) {
                        val x0 = entry.x + quad.x0 * scale
                        val y0 = entry.y + quad.y0 * scale
                        val x1 = entry.x + quad.x1 * scale
                        val y1 = entry.y + quad.y1 * scale

                        quad(entry, dpr, x0, y0, x1, y1, quad.u0, quad.v0, quad.u1, quad.v1, entry.color)
                    }
                }.buildOrThrow().use { mesh ->
                    device.createBuffer({ "lumina image" }, GpuBuffer.USAGE_VERTEX, mesh.vertexBuffer()).use { buffer ->
                        val texView = font.atlas.textureView ?: return@use

                        pass.usePipeline(getPipeline(texts))
                        pass.setVertexBuffer(0, buffer /*? if > 26.1 {*//*.slice()*//*?}*/)
                        /*? if >= 26.3 {*/ /*pass.setUniform("Sampler0", texView, sampler) *//*?} else {*/ pass.bindTexture("Sampler0", texView, sampler) /*?}*/
                        pass.draw(/*? if < 26.2 { */ 0, vertices /*? } else { *//*vertices, 1, 0, 0 *//*? } */)
                    }
                }
            }
        }
    }

    private fun getPipeline(batch: List<LuminaV2.Entry>) = when(batch.first().mask) {
        Mask.Write -> LuminaPipelines.TEXTURE_WRITE
        Mask.In -> LuminaPipelines.TEXTURE_IN
        Mask.None -> LuminaPipelines.TEXTURE
    }

    fun BufferBuilder.quad(entry: LuminaV2.Entry, dpr: Float, x0: Float, y0: Float, x1: Float, y1: Float, u0: Float, v0: Float, u1: Float, v1: Float, color: Int) {
        val tl = entry.toScreen(x0, y0, dpr)
        val tr = entry.toScreen(x1, y0, dpr)
        val br = entry.toScreen(x1, y1, dpr)
        val bl = entry.toScreen(x0, y1, dpr)

        addVertex(tl.x, tl.y, 0f).setUv(u0, v0).setColor(color)
        addVertex(tr.x, tr.y, 0f).setUv(u1, v0).setColor(color)
        addVertex(bl.x, bl.y, 0f).setUv(u0, v1).setColor(color)

        addVertex(br.x, br.y, 0f).setUv(u1, v1).setColor(color)
        addVertex(bl.x, bl.y, 0f).setUv(u0, v1).setColor(color)
        addVertex(tr.x, tr.y, 0f).setUv(u1, v0).setColor(color)
    }
}