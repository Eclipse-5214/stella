package co.stellarskys.stella.utils.render

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.render.*
import net.minecraft.util.shape.VoxelShape
import org.joml.Matrix4f
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.render.world.RenderContext
import java.awt.Color
import kotlin.math.sqrt

object Render3D {
    fun renderFilled(
        ctx: RenderContext,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers()
        val matrices = ctx.matrixStack() ?: return
        val layer = if (phase) StellaRenderLayers.FILLEDTHROUGHWALLS else StellaRenderLayers.FILLED

        // TODO: make this more efficient later on
        //  (this does way too many calls but shouldn't matter much as of right now)
        shape.forEachBox { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
            VertexRendering.drawFilledBox(
                matrices,
                consumers.getBuffer(layer),
                minX + ox, minY + oy, minZ + oz,
                maxX + ox, maxY + oy, maxZ + oz,
                color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
            )
        }
    }

    fun renderOutline(
        ctx: RenderContext,
        shape: VoxelShape,
        ox: Double,
        oy: Double,
        oz: Double,
        color: Color,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers()
        val matrices = ctx.matrixStack() ?: return
        val layer = if (phase) StellaRenderLayers.getLinesThroughWalls( 1.0) else StellaRenderLayers.getLines( 1.0)

        VertexRendering.drawOutline(
            matrices,
            consumers.getBuffer(layer),
            shape,
            ox, oy, oz,
            color.rgb
        )
    }

    fun renderBox(
        ctx: RenderContext,
        x: Double,
        y: Double,
        z: Double,
        width: Double,
        height: Double,
        color: Color = Color.CYAN,
        phase: Boolean = false,
        lineWidth: Double = 1.0
    ) {
        val consumers = ctx.consumers()
        val matrices = ctx.matrixStack() ?: return
        val cam = ctx.camera().pos.negate()
        val layer = if (phase) StellaRenderLayers.getLinesThroughWalls(lineWidth) else StellaRenderLayers.getLines(lineWidth)
        val cx = x + 0.5
        val cz = z + 0.5
        val halfWidth = width / 2

        matrices.push()
        matrices.translate(cam.x, cam.y, cam.z)

        VertexRendering.drawBox(
            matrices,
            consumers.getBuffer(layer),
            cx - halfWidth, y, cz - halfWidth,
            cx + halfWidth, y + height, cz + halfWidth,
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
        )

        matrices.pop()
    }

    fun renderFilledBox(
        ctx: RenderContext,
        x: Double,
        y: Double,
        z: Double,
        width: Double,
        height: Double,
        color: Color = Color.CYAN,
        phase: Boolean = false
    ) {
        val consumers = ctx.consumers()
        val matrices = ctx.matrixStack() ?: return
        val cam = ctx.camera().pos.negate()
        val layer = if (phase) StellaRenderLayers.FILLEDTHROUGHWALLS else StellaRenderLayers.FILLED
        val cx = x + 0.5
        val cz = z + 0.5
        val halfWidth = width / 2

        matrices.push()
        matrices.translate(cam.x, cam.y, cam.z)

        VertexRendering.drawFilledBox(
            matrices,
            consumers.getBuffer(layer),
            cx - halfWidth, y, cz - halfWidth,
            cx + halfWidth, y + height, cz + halfWidth,
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
        )

        matrices.pop()
    }

    fun renderString(
        text: String,
        x: Double,
        y: Double,
        z: Double,
        scale: Float = 1f,
        bgBox: Boolean = false,
        increase: Boolean = false,
        phase: Boolean = false
    ) {
        var toScale = scale
        val matrices = Matrix4f()
        val textRenderer = KnitClient.client.textRenderer
        val camera = KnitClient.client.gameRenderer.camera
        val dx = (x - camera.pos.x).toFloat()
        val dy = (y - camera.pos.y).toFloat()
        val dz = (z - camera.pos.z).toFloat()

        toScale *= if (increase) sqrt(dx * dx + dy * dy + dz * dz) / 120f else 0.025f

        matrices
            .translate(dx, dy, dz)
            .rotate(camera.rotation)
            .scale(toScale, -toScale, toScale)

        val consumer = KnitClient.client.bufferBuilders.entityVertexConsumers
        val textLayer = if (phase) TextRenderer.TextLayerType.SEE_THROUGH else TextRenderer.TextLayerType.NORMAL
        val lines = text.split("\n")
        val maxWidth = lines.maxOf { textRenderer.getWidth(it) }
        val offset = -maxWidth / 2f

        if (bgBox) {
            val bgOpacity = (KnitClient.client.options.getTextBackgroundOpacity(0.25f) * 255).toInt() shl 24
            val boxHeight = lines.size * 9
            textRenderer.draw(
                "", // empty string, just draws box
                offset,
                0f,
                0x20FFFFFF,
                true,
                matrices,
                consumer,
                textLayer,
                bgOpacity,
                LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE
            )
        }

        for ((i, line) in lines.withIndex()) {
            val lineWidth = textRenderer.getWidth(line)
            val lineOffset = -lineWidth / 2f

            textRenderer.draw(
                line,
                lineOffset,
                i * 9f,
                0xFFFFFFFF.toInt(),
                true,
                matrices,
                consumer,
                textLayer,
                0,
                LightmapTextureManager.MAX_BLOCK_LIGHT_COORDINATE
            )
        }

        consumer.draw()
    }
}