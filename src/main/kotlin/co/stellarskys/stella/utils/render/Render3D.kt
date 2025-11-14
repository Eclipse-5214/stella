package co.stellarskys.stella.utils.render

import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.world.phys.shapes.VoxelShape
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
        val layer = if (phase) StellaRenderLayers.FILLED_THROUGH_WALLS else StellaRenderLayers.FILLED

        // TODO: make this more efficient later on
        //  (this does way too many calls but shouldn't matter much as of right now)
        shape.forAllBoxes { minX: Double, minY: Double, minZ: Double, maxX: Double, maxY: Double, maxZ: Double ->
            ShapeRenderer.addChainedFilledBoxVertices(
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

        ShapeRenderer.renderShape(
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
        val cam = ctx.camera().position.reverse()
        val layer = if (phase) StellaRenderLayers.getLinesThroughWalls(lineWidth) else StellaRenderLayers.getLines(lineWidth)
        val cx = x + 0.5
        val cz = z + 0.5
        val halfWidth = width / 2

        matrices.popPose()
        matrices.translate(cam.x, cam.y, cam.z)

        ShapeRenderer.renderLineBox(
            matrices,
            consumers.getBuffer(layer),
            cx - halfWidth, y, cz - halfWidth,
            cx + halfWidth, y + height, cz + halfWidth,
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
        )

        matrices.popPose()
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
        val cam = ctx.camera().position.reverse()
        val layer = if (phase) StellaRenderLayers.FILLED_THROUGH_WALLS else StellaRenderLayers.FILLED
        val cx = x + 0.5
        val cz = z + 0.5
        val halfWidth = width / 2

        matrices.pushPose()
        matrices.translate(cam.x, cam.y, cam.z)

        ShapeRenderer.addChainedFilledBoxVertices(
            matrices,
            consumers.getBuffer(layer),
            cx - halfWidth, y, cz - halfWidth,
            cx + halfWidth, y + height, cz + halfWidth,
            color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f
        )

        matrices.popPose()
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
        val textRenderer = KnitClient.client.font
        val camera = KnitClient.client.gameRenderer.mainCamera
        val dx = (x - camera.position.x).toFloat()
        val dy = (y - camera.position.y).toFloat()
        val dz = (z - camera.position.z).toFloat()

        toScale *= if (increase) sqrt(dx * dx + dy * dy + dz * dz) / 120f else 0.025f

        matrices
            .translate(dx, dy, dz)
            .rotate(camera.rotation())
            .scale(toScale, -toScale, toScale)

        val consumer = KnitClient.client.renderBuffers().bufferSource()
        val textLayer = if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL
        val lines = text.split("\n")
        val maxWidth = lines.maxOf { textRenderer.width(it) }
        val offset = -maxWidth / 2f

        if (bgBox) {
            val bgOpacity = (KnitClient.client.options.getBackgroundOpacity(0.25f) * 255).toInt() shl 24
            val boxHeight = lines.size * 9
            textRenderer.drawInBatch(
                "", // empty string, just draws box
                offset,
                0f,
                0x20FFFFFF,
                true,
                matrices,
                consumer,
                textLayer,
                bgOpacity,
                LightTexture.FULL_BLOCK
            )
        }

        for ((i, line) in lines.withIndex()) {
            val lineWidth = textRenderer.width(line)
            val lineOffset = -lineWidth / 2f

            textRenderer.drawInBatch(
                line,
                lineOffset,
                i * 9f,
                0xFFFFFFFF.toInt(),
                true,
                matrices,
                consumer,
                textLayer,
                0,
                LightTexture.FULL_BLOCK
            )
        }

        consumer.endBatch()
    }
}