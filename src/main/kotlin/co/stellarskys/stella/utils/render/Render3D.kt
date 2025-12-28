package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.WorldUtils
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.ShapeRenderer
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.Style
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.EmptyBlockGetter
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
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
        pos: Vec3,
        scale: Float = 1f,
        bgBox: Boolean = false,
        increase: Boolean = false,
        phase: Boolean = false
    ) {
        renderString(text, pos.x, pos.y, pos.z, scale, bgBox, increase, phase)
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
            val widestLine = lines.maxByOrNull { textRenderer.width(it) } ?: ""
            for ((i, _) in lines.withIndex()) {
                textRenderer.drawInBatch(
                    widestLine,
                    offset,
                    i * 9f,
                    0x20FFFFFF,
                    true,
                    matrices,
                    consumer,
                    textLayer,
                    (client.options.getBackgroundOpacity(0.25f) * 255).toInt() shl 24,
                    LightTexture.FULL_BLOCK
                )
            }
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

    fun renderLine(
        start: Vec3,
        finish: Vec3,
        thickness: Float,
        color: Color,
        consumers: MultiBufferSource?,
        matrixStack: PoseStack?
    ) {
        val cameraPos = client.gameRenderer.mainCamera.position
        val matrices = matrixStack ?: return
        matrices.pushPose()
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)
        val entry = matrices.last()
        val consumers = consumers as MultiBufferSource.BufferSource
        val buffer = consumers.getBuffer(RenderType.lines())

        RenderSystem.lineWidth(thickness)

        val r = color.red / 255f
        val g = color.green / 255f
        val b = color.blue / 255f
        val a = color.alpha / 255f

        val direction = finish.subtract(start).normalize().toVector3f()

        buffer.addVertex(entry, start.x.toFloat(), start.y.toFloat(), start.z.toFloat())
            .setColor(r, g, b, a)
            .setNormal(entry, direction)

        buffer.addVertex(entry, finish.x.toFloat(), finish.y.toFloat(), finish.z.toFloat())
            .setColor(r, g, b, a)
            .setNormal(entry, direction)

        consumers.endBatch(RenderType.lines())
        matrices.popPose()
    }

    fun renderLineFromCursor(
        consumers: MultiBufferSource?,
        matrixStack: PoseStack?,
        point: Vec3,
        colorComponents: FloatArray,
        alpha: Float
    ) {
        val camera = client.gameRenderer.mainCamera
        val cameraPos = camera.position
        matrixStack?.pushPose()
        matrixStack?.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z)
        val entry = matrixStack?.last()
        val consumers = consumers as MultiBufferSource.BufferSource
        val layer = RenderType.lines()
        val buffer = consumers.getBuffer(layer)

        val cameraPoint = cameraPos.add(Vec3.directionFromRotation(camera.xRot, camera.yRot))
        val normal = point.toVector3f().sub(cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat()).normalize()

        buffer.addVertex(entry, cameraPoint.x.toFloat(), cameraPoint.y.toFloat(), cameraPoint.z.toFloat())
            .setColor(colorComponents[0], colorComponents[1], colorComponents[2], alpha)
            .setNormal(entry, normal)

        buffer.addVertex(entry, point.x.toFloat(), point.y.toFloat(), point.z.toFloat())
            .setColor(colorComponents[0], colorComponents[1], colorComponents[2], alpha)
            .setNormal(entry, normal)

        consumers.endBatch(layer)
        matrixStack?.popPose()
    }
}