package co.stellarskys.stella.api.astrum

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.RenderEvent
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import net.minecraft.client.Camera
import net.minecraft.client.gui.Font
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import java.awt.Color

//?if > 1.21.10 {
 import net.minecraft.client.renderer.rendertype.RenderType
 //? } else {
/*import net.minecraft.client.renderer.RenderType
*///? }

@Module
object Astrum {
    data class QueuedText(val text: String, val pos: Vec3, val color: Int, val scale: Float, val shadow: Boolean, val depth: Boolean, val bgColor: Int = 0)
    data class QueuedVoxel(val shape: VoxelShape, val pos: Vec3, val color: Color, val depth: Boolean, val lineWidth: Float = 1f)
    data class QueuedLine(val start: Vec3, val end: Vec3, val color: Color, val width: Float, val depth: Boolean)

    private val outlineVoxelQueue = mutableListOf<QueuedVoxel>()
    private val filledVoxelQueue = mutableListOf<QueuedVoxel>()
    private val lineQueue = mutableListOf<QueuedLine>()
    private val textQueue = mutableListOf<QueuedText>()

    lateinit var buffers: MultiBufferSource.BufferSource
    lateinit var cam: Camera

    init {
        EventBus.on<RenderEvent.World.Last> { event ->
            if (lineQueue.isEmpty() && textQueue.isEmpty() && outlineVoxelQueue.isEmpty() && filledVoxelQueue.isEmpty()) return@on
            cam = client.gameRenderer.mainCamera
            buffers = client.renderBuffers().bufferSource()
            flush(event.matrices)
        }
    }

    // Queueing
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

    // Actually rendering
    fun flush(pose: PoseStack) {
        val pos = cam.position()
        pose.pushPose()
        pose.translate(-pos.x, -pos.y, -pos.z)
        val poseEntry = pose.last()

        if (outlineVoxelQueue.isNotEmpty()) {
            val (depth, noDepth) = outlineVoxelQueue.partition { it.depth }
            renderVoxelBatch(depth, AstrumLayers.getLines(true), poseEntry) { b, p, v ->
                addVoxelOutlineVertices(b, p, v)
            }

            renderVoxelBatch(noDepth, AstrumLayers.getLines(false), poseEntry) { b, p, v ->
                addVoxelOutlineVertices(b, p, v)
            }
        }

        if (filledVoxelQueue.isNotEmpty()) {
            val (depth, noDepth) = filledVoxelQueue.partition { it.depth }
            renderVoxelBatch(depth, AstrumLayers.getFilled(true), poseEntry) { b, p, v ->
                addVoxelFillVertices(b, p, v)
            }
            renderVoxelBatch(noDepth, AstrumLayers.getFilled(false), poseEntry) { b, p, v ->
                addVoxelFillVertices(b, p, v)
            }
        }

        if (lineQueue.isNotEmpty()) {
            val (depth, noDepth) = lineQueue.partition { it.depth }

            depth.groupBy { it.width }.forEach { (width, lines) ->
                renderLineBatch(lines, AstrumLayers.getLines(true), poseEntry)
            }

            noDepth.groupBy { it.width }.forEach { (width, lines) ->
                renderLineBatch(lines, AstrumLayers.getLines(false), poseEntry)
            }
        }

        if (textQueue.isNotEmpty()) renderTextBatch(textQueue, pose)
        pose.popPose()
        buffers.endBatch()

        lineQueue.clear()
        textQueue.clear()
        outlineVoxelQueue.clear()
        filledVoxelQueue.clear()
    }

    // Batch Renderers
    fun renderVoxelBatch(
        batch: List<QueuedVoxel>,
        layer: RenderType,
        pose: PoseStack.Pose,
        processor: (buffer: VertexConsumer, pose: PoseStack.Pose, QueuedVoxel) -> Unit
    ) {
        if (batch.isEmpty()) return
        val buffer = buffers.getBuffer(layer)
        batch.forEach { processor(buffer, pose, it) }
    }

    fun renderLineBatch(
        batch: List<QueuedLine>,
        layer: RenderType,
        pose: PoseStack.Pose
    ) {
        if (batch.isEmpty()) return
        val buffer = buffers.getBuffer(layer)
        batch.forEach { addLineVertices(buffer, pose, it) }
    }

    fun renderTextBatch(batch: List<QueuedText>, pose: PoseStack) {
        if (batch.isEmpty()) return

        val font = client.font
        val source = client.renderBuffers().bufferSource()

        batch.forEach { queued ->
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
    }

    // Vertex Processors
    fun addVoxelOutlineVertices(buffer: VertexConsumer, pose: PoseStack.Pose, queued: QueuedVoxel) {
        val pos = queued.pos
        val color = queued.color.rgb

        queued.shape.forAllEdges { x1, y1, z1, x2, y2, z2 ->
            val sx = (x1 + pos.x).toFloat(); val sy = (y1 + pos.y).toFloat(); val sz = (z1 + pos.z).toFloat()
            val ex = (x2 + pos.x).toFloat(); val ey = (y2 + pos.y).toFloat(); val ez = (z2 + pos.z).toFloat()

            buffer.addVertex(pose, sx, sy, sz).setColor(color).setNormal(pose, ex - sx, ey - sy, ez - sz).setLineWidth(queued.lineWidth)
            buffer.addVertex(pose, ex, ey, ez).setColor(color).setNormal(pose, ex - sx, ey - sy, ez - sz).setLineWidth(queued.lineWidth)
        }
    }

    fun addVoxelFillVertices(buffer: VertexConsumer, pose: PoseStack.Pose, queued: QueuedVoxel) {
        val (shape, pos, color) = queued
        val offset = 0.0001
        shape.forAllBoxes { x1, y1, z1, x2, y2, z2 ->
            addFilledBoxVertices(
                buffer, pose,
                (x1 + pos.x - offset).toFloat(), (y1 + pos.y - offset).toFloat(), (z1 + pos.z - offset).toFloat(),
                (x2 + pos.x + offset).toFloat(), (y2 + pos.y + offset).toFloat(), (z2 + pos.z + offset).toFloat(),
                color
            )
        }
    }

    fun addLineVertices(buffer: VertexConsumer, pose: PoseStack.Pose, line: QueuedLine) {
        val s = line.start
        val e = line.end
        val color = line.color.rgb

        val nx = e.xf - s.xf
        val ny = e.yf - s.yf
        val nz = e.zf - s.zf

        buffer.addVertex(pose, s.xf, s.yf, s.zf).setColor(color).setNormal(pose, nx, ny, nz) /*? if > 1.21.10 { */.setLineWidth(line.width) /*?}*/
        buffer.addVertex(pose, e.xf, e.yf, e.zf).setColor(color).setNormal(pose, nx, ny, nz) /*? if > 1.21.10 { */.setLineWidth(line.width) /*?}*/
    }

    fun addFilledBoxVertices(
        buffer: VertexConsumer,
        pose: PoseStack.Pose,
        x0: Float, y0: Float, z0: Float,
        x1: Float, y1: Float, z1: Float,
        color: Color
    ) {
        val col = color.rgb

        val c = arrayOf(
            floatArrayOf(x0, y0, z0), floatArrayOf(x1, y0, z0),
            floatArrayOf(x1, y1, z0), floatArrayOf(x0, y1, z0),
            floatArrayOf(x0, y0, z1), floatArrayOf(x1, y0, z1),
            floatArrayOf(x1, y1, z1), floatArrayOf(x0, y1, z1)
        )

        val faces = arrayOf(
            intArrayOf(0, 4, 7, 3), intArrayOf(5, 1, 2, 6),
            intArrayOf(0, 3, 2, 1), intArrayOf(5, 6, 7, 4),
            intArrayOf(0, 1, 5, 4), intArrayOf(7, 6, 2, 3)
        )

        for (face in faces) {
            for (index in face) {
                val vert = c[index]
                buffer.addVertex(pose, vert[0], vert[1], vert[2]).setColor(col)
            }
        }
    }

    inline val Vec3.xf: Float get() = x.toFloat()
    inline val Vec3.yf: Float get() = y.toFloat()
    inline val Vec3.zf: Float get() = z.toFloat()
}