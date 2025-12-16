package co.stellarskys.stella.utils.render

import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.core.BlockPos
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.joml.Matrix4f
import java.awt.Color
import kotlin.math.sqrt

object Render3D {
    fun outlineBlock(
        ctx: RenderContext,
        pos: BlockPos,
        color: Color,
        lineWidth: Double = 1.0,
        phase: Boolean = false,
        blockState: BlockState? = null
    ) {
        val finalState = blockState ?: WorldUtils.getBlockStateAt(pos.x, pos.y, pos.z) ?: return
        val mstack = ctx.matrixStack ?: return
        val consumers = ctx.consumers ?: return
        val camera = ctx.camera
        val camPos = camera.position
        val blockShape = finalState.getShape(EmptyBlockGetter.INSTANCE, pos, CollisionContext.of(camera.entity))
        if (blockShape.isEmpty) {
            renderBox(ctx, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 1.0, 1.0, color, phase, lineWidth)
            return
        }

        ShapeRenderer.renderShape(
            mstack,
            consumers.getBuffer(if (phase) StellaRenderLayers.getLinesThroughWalls(lineWidth) else StellaRenderLayers.getLines(lineWidth)),
            blockShape,
            pos.x - camPos.x, pos.y - camPos.y, pos.z - camPos.z,
            color.rgb
        )
    }

    fun fillBlock(
        ctx: RenderContext,
        pos: BlockPos,
        color: Color,
        phase: Boolean = false,
        blockState: BlockState? = null
    ) {
        val blockState = WorldUtils.getBlockStateAt(pos.x, pos.y, pos.z) ?: return
        val mstack = ctx.matrixStack ?: return
        val consumers = ctx.consumers ?: return
        val camera = ctx.camera
        val camPos = camera.position
        val blockShape = blockState.getShape(EmptyBlockGetter.INSTANCE, pos, CollisionContext.of(camera.entity))

        if (blockShape.isEmpty) {
            renderFilledBox(ctx, pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 1.0, 1.0, color, phase)
            return
        }

        blockShape.forAllBoxes { minX, minY, minZ, maxX, maxY, maxZ ->
            ShapeRenderer.addChainedFilledBoxVertices(
                mstack,
                consumers.getBuffer( if (phase) StellaRenderLayers.FILLED_THROUGH_WALLS else StellaRenderLayers.FILLED),
                pos.x - camPos.x + minX, pos.y - camPos.y + minY, pos.z - camPos.z + minZ,
                pos.x - camPos.x + maxX, pos.y - camPos.y + maxY, pos.z - camPos.z + maxZ,
                color.red / 255f,color.green / 255f,color.blue / 255f,color.alpha / 255f
            )
        }
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
        val consumers = ctx.consumers ?: return
        val matrices = ctx.matrixStack ?: return
        val cam = ctx.camera.position.reverse()
        val layer = if (phase) StellaRenderLayers.getLinesThroughWalls(lineWidth) else StellaRenderLayers.getLines(lineWidth)
        val cx = x + 0.5
        val cz = z + 0.5
        val halfWidth = width / 2

        matrices.pushPose()
        matrices.translate(cam.x, cam.y, cam.z)

        ShapeRenderer.renderLineBox(
            //#if MC >= 1.21.9
            //$$ matrices.last(),
            //#else
            matrices,
            //#endif
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
        val consumers = ctx.consumers ?: return
        val matrices = ctx.matrixStack ?: return
        val cam = ctx.camera.position.reverse()
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
        val textRenderer = client.font
        val camera = client.gameRenderer.mainCamera
        val dx = (x - camera.position.x).toFloat()
        val dy = (y - camera.position.y).toFloat()
        val dz = (z - camera.position.z).toFloat()

        toScale *= if (increase) sqrt(dx * dx + dy * dy + dz * dz) / 120f else 0.025f

        matrices
            .translate(dx, dy, dz)
            .rotate(camera.rotation())
            .scale(toScale, -toScale, toScale)

        val consumer = client.renderBuffers().bufferSource()
        val textLayer = if (phase) Font.DisplayMode.SEE_THROUGH else Font.DisplayMode.NORMAL
        val lines = text.split("\n")
        val maxWidth = lines.maxOf { textRenderer.width(it) }
        val offset = -maxWidth / 2f

        if (bgBox) {
            val bgOpacity = (client.options.getBackgroundOpacity(0.25f) * 255).toInt() shl 24
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