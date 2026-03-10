package co.stellarskys.stella.utils.render

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.RenderEvent
import com.mojang.blaze3d.vertex.PoseStack
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipeline
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStack
import dev.deftu.omnicore.api.client.render.stack.OmniPoseStacks
import dev.deftu.omnicore.api.client.render.vertex.OmniBufferBuilder
import net.minecraft.client.Camera
import net.minecraft.client.gui.Font
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.awt.Color
import kotlin.math.sign

//? > 1.21.10 {
/*import net.minecraft.gizmos.Gizmos
*///? }


@Module
object RenderQue {
    data class QueuedText(val text: String, val pos: Vec3, val color: Int, val scale: Float, val shadow: Boolean, val depth: Boolean, val bgColor: Int = 0)
    data class QueuedVoxel(val shape: VoxelShape, val pos: Vec3, val color: Color, val depth: Boolean, val lineWidth: Float = 1f)
    data class QueuedLine(val start: Vec3, val end: Vec3, val color: Color, val width: Float, val depth: Boolean)

    private val outlineVoxelQueue = mutableListOf<QueuedVoxel>()
    private val filledVoxelQueue = mutableListOf<QueuedVoxel>()
    private val lineQueue = mutableListOf<QueuedLine>()
    private val textQueue = mutableListOf<QueuedText>()
    lateinit var cam: Camera

    init {
        EventBus.on<RenderEvent.World.Last> { event ->
            if (lineQueue.isEmpty() && textQueue.isEmpty() && outlineVoxelQueue.isEmpty() && filledVoxelQueue.isEmpty()) return@on
            cam = client.gameRenderer.mainCamera
            flush(event.matrices)
        }
    }

    fun queueVoxelOutline(shape: VoxelShape, pos: Vec3, color: Color, depth: Boolean = true, lineWidth: Float = 1f) {
        outlineVoxelQueue.add(QueuedVoxel(shape, pos, color, depth, lineWidth))
    }

    fun queueVoxelFill(shape: VoxelShape, pos: Vec3, color: Color, depth: Boolean = true) {
        filledVoxelQueue.add(QueuedVoxel(shape, pos, color, depth))
    }

    fun queueBox(aabb: AABB, color: Color, filled: Boolean = false, depth: Boolean = true, lineWidth: Float = 1f) {
        val shape = Shapes.create(aabb)
        if (filled) queueVoxelFill(shape, Vec3.ZERO, color, depth)
        else queueVoxelOutline(shape, Vec3.ZERO, color, depth, lineWidth)
    }

    fun queueLine(start: Vec3, end: Vec3, color: Color, width: Float = 1f, depth: Boolean = true) {
        lineQueue.add(QueuedLine(start, end, color, width, depth))
    }

    fun queueText(text: String, pos: Vec3, color: Int = 0xFFFFFFFF.toInt(), scale: Float = 1f, shadow: Boolean = true, depth: Boolean = true, bgColor: Int = 0) {
        textQueue.add(QueuedText(text, pos, color, scale, shadow, depth, bgColor))
    }

    private fun addVoxelOutlineVertices(buffer: OmniBufferBuilder, pose: OmniPoseStack, queued: QueuedVoxel) {
        val (shape, pos, color) = queued
        shape.forAllEdges { x1, y1, z1, x2, y2, z2 ->
            val startX = x1 + pos.x; val startY = y1 + pos.y; val startZ = z1 + pos.z
            val endX = x2 + pos.x; val endY = y2 + pos.y; val endZ = z2 + pos.z

            val nx = sign(endX - startX).toFloat()
            val ny = sign(endY - startY).toFloat()
            val nz = sign(endZ - startZ).toFloat()

            buffer.vertex(pose, startX, startY, startZ).color(color).normal(pose, nx, ny, nz).next()
            buffer.vertex(pose, endX, endY, endZ).color(color).normal(pose, nx, ny, nz).next()
        }
    }

    private fun addVoxelFillVertices(buffer: OmniBufferBuilder, pose: OmniPoseStack, queued: QueuedVoxel) {
        val (shape, pos, color) = queued
        val offset = 0.0001
        shape.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
            addFilledBoxVertices(
                buffer, pose,
                x1 + pos.x - offset, y1 + pos.y - offset, z1 + pos.z - offset,
                x2 + pos.x + offset, y2 + pos.y + offset, z2 + pos.z + offset,
                color
            )
        }
    }

    private fun addLineVertices(buffer: OmniBufferBuilder, pose: OmniPoseStack, line: QueuedLine) {
        val start = line.start
        val end = line.end
        val color = line.color

        val nx = sign(end.x - start.x).toFloat()
        val ny = sign(end.y - start.y).toFloat()
        val nz = sign(end.z - start.z).toFloat()

        buffer.vertex(pose, start.x, start.y, start.z).color(color).normal(pose, nx, ny, nz).next()
        buffer.vertex(pose, end.x, end.y, end.z).color(color).normal(pose, nx, ny, nz).next()
    }

    fun flush(pose: PoseStack) {
        val pos = cam.position()
        pose.pushPose()
        pose.translate(-pos.x, -pos.y, -pos.z)
        val omniPose = OmniPoseStacks.vanilla(pose)

        if (outlineVoxelQueue.isNotEmpty()) {
            val (depth, noDepth) = outlineVoxelQueue.partition { it.depth }

            renderVoxelBatch(depth, StellaRenderPipelines.LINES, omniPose) { b, p, v ->
                addVoxelOutlineVertices(b, p, v)
            }
            renderVoxelBatch(noDepth, StellaRenderPipelines.LINES_THROUGH_WALLS, omniPose) { b, p, v ->
                addVoxelOutlineVertices(b, p, v)
            }
        }

        if (filledVoxelQueue.isNotEmpty()) {
            val (depth, noDepth) = filledVoxelQueue.partition { it.depth }

            renderVoxelBatch(depth, StellaRenderPipelines.FILLED, omniPose) { b, p, v ->
                addVoxelFillVertices(b, p, v)
            }
            renderVoxelBatch(noDepth, StellaRenderPipelines.FILLED_THROUGH_WALLS, omniPose) { b, p, v ->
                addVoxelFillVertices(b, p, v)
            }
        }

        //? > 1.21.10 {
        /*lineQueue.forEach { l ->
            Gizmos.line(l.start, l.end, l.color.rgb, l.width).apply {
                if (!l.depth) setAlwaysOnTop()
            }
        }
        *///? } else {
         if (lineQueue.isNotEmpty()) {
            val (depth, noDepth) = lineQueue.partition { it.depth }

            depth.groupBy { it.width }.forEach { (width, lines) ->
                val buffer = StellaRenderPipelines.LINES.createBufferBuilder()
                lines.forEach { addLineVertices(buffer, omniPose, it) }
                buffer.buildOrThrow().drawAndClose(StellaRenderPipelines.LINES) { setLineWidth(width) }
            }

            noDepth.groupBy { it.width }.forEach { (width, lines) ->
                val buffer = StellaRenderPipelines.LINES_THROUGH_WALLS.createBufferBuilder()
                lines.forEach { addLineVertices(buffer, omniPose, it) }
                buffer.buildOrThrow().drawAndClose(StellaRenderPipelines.LINES_THROUGH_WALLS) { setLineWidth(width) }
            }
        }
        //?}

        if (textQueue.isNotEmpty()) renderTextBatch(pose)
        pose.popPose()

        lineQueue.clear()
        textQueue.clear()
        outlineVoxelQueue.clear()
        filledVoxelQueue.clear()
    }

    private fun renderVoxelBatch(
        batch: List<QueuedVoxel>,
        pipeline: OmniRenderPipeline,
        pose: OmniPoseStack,
        processor: (OmniBufferBuilder, OmniPoseStack, QueuedVoxel) -> Unit
    ) {
        if (batch.isEmpty()) return
        //? > 1.21.10 {
        /*if (pipeline.location.toString().contains("lines")) {
            batch.forEach { (shape, pos, color, depth, lineWidth) -> shape.forAllEdges { x1, y1, z1, x2, y2, z2 ->
                val start = Vec3(x1, y1, z1).add(pos); val end = Vec3(x2, y2, z2).add(pos)
                Gizmos.line(start, end, color.rgb, lineWidth).apply { if (!depth) setAlwaysOnTop() }
            }}; return
        }
        *///?}
        val buffer = pipeline.createBufferBuilder()
        batch.forEach { processor(buffer, pose, it) }
        val width = batch.first().lineWidth
        buffer.buildOrThrow().drawAndClose(pipeline) { setLineWidth(width) }
    }

    private fun addFilledBoxVertices(
        buffer: OmniBufferBuilder,
        pose: OmniPoseStack,
        x0: Double, y0: Double, z0: Double,
        x1: Double, y1: Double, z1: Double,
        color: Color
    ) {
        val verts = arrayOf(
            doubleArrayOf(x1, y1, z0), doubleArrayOf(x1, y1, z0),
            doubleArrayOf(x0, y1, z0), doubleArrayOf(x1, y1, z1),
            doubleArrayOf(x0, y1, z1), doubleArrayOf(x0, y0, z1),
            doubleArrayOf(x0, y1, z0), doubleArrayOf(x0, y0, z0),
            doubleArrayOf(x1, y1, z0), doubleArrayOf(x1, y0, z0),
            doubleArrayOf(x1, y1, z1), doubleArrayOf(x1, y0, z1),
            doubleArrayOf(x0, y0, z1), doubleArrayOf(x1, y0, z0),
            doubleArrayOf(x0, y0, z0), doubleArrayOf(x0, y0, z0)
        )

        for (v in verts) buffer.vertex(pose, v[0], v[1], v[2]).color(color).next()
    }

    private fun renderTextBatch(pose: PoseStack) {
        val font = client.font
        val source = client.renderBuffers().bufferSource()

        textQueue.forEach { queued ->
            pose.pushPose()
            pose.translate(queued.pos.x, queued.pos.y, queued.pos.z)
            pose.mulPose(cam.rotation())

            val s = queued.scale * 0.025f
            pose.scale(s, -s, s)

            val textWidth = font.width(queued.text)
            val xOffset = -textWidth / 2f
            val matrix = pose.last().pose()

            font.drawInBatch(
                queued.text,
                xOffset, 0f,
                queued.color,
                queued.shadow,
                matrix,
                source,
                if (queued.depth) Font.DisplayMode.NORMAL else Font.DisplayMode.SEE_THROUGH,
                queued.bgColor,
                0xF000F0
            )

            pose.popPose()
        }
        source.endBatch()
    }
}