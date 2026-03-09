package co.stellarskys.stella.utils.render

import dev.deftu.omnicore.api.client.client
import net.minecraft.client.Camera
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import net.minecraft.world.phys.shapes.CollisionContext
import java.awt.Color

object Render3D {
    //? <= 1.21.10 {
    fun Camera.xRot() = this.xRot
    fun Camera.yRot() = this.yRot
    //?}

    fun outlineBlock(pos: BlockPos, color: Color, lineWidth: Float = 1f, depth: Boolean = true, state: BlockState? = null) {
        val level = client.level ?: return
        val finalState = state ?:level.getBlockState(pos)
        val shape = finalState.getShape(level, pos, CollisionContext.empty())

        if (shape.isEmpty) {
            drawBox(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 1.0, 1.0, color, depth, lineWidth)
            return
        }

        RenderQue.queueVoxelOutline(shape, Vec3.atLowerCornerOf(pos), color, depth, lineWidth)
    }

    fun fillBlock(pos: BlockPos, color: Color, depth: Boolean = true, state: BlockState? = null) {
        val level = client.level ?: return
        val finalState = state ?: level.getBlockState(pos)
        val shape = finalState.getShape(level, pos, CollisionContext.empty())

        if (shape.isEmpty) {
            drawFilledBox(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble(), 1.0, 1.0, color, depth)
            return
        }

        RenderQue.queueVoxelFill(shape, Vec3.atLowerCornerOf(pos), color, depth)
    }

    fun drawBox(x: Double, y: Double, z: Double, width: Double, height: Double, color: Color, depth: Boolean = true, lineWidth: Float = 1f) {
        val hw = width / 2.0
        val aabb = AABB(x + 0.5 - hw, y, z + 0.5 - hw, x + 0.5 + hw, y + height, z + 0.5 + hw)
        RenderQue.queueBox(aabb, color, filled = false, depth = depth, lineWidth = lineWidth)
    }

    fun drawFilledBox(x: Double, y: Double, z: Double, width: Double, height: Double, color: Color, depth: Boolean = true) {
        val hw = width / 2.0
        val aabb = AABB(x + 0.5 - hw, y, z + 0.5 - hw, x + 0.5 + hw, y + height, z + 0.5 + hw)
        RenderQue.queueBox(aabb, color, filled = true, depth = depth)
    }

    fun drawLine(start: Vec3, finish: Vec3, thickness: Float, color: Color, depth: Boolean = true) {
        RenderQue.queueLine(start, finish, color, thickness, depth)
    }

    fun drawLineFromCursor(target: Vec3, color: Color, width: Float = 1f) {
        val cam = RenderQue.cam
        val start = cam.position().add(Vec3.directionFromRotation(cam.xRot(), cam.yRot()).scale(0.5))
        RenderQue.queueLine(start, target, color, width, depth = false)
    }

    fun drawText(
        text: String,
        x: Double,
        y: Double,
        z: Double,
        scale: Float = 1f,
        color: Int = 0xFFFFFFFF.toInt(),
        bgBox: Boolean = false,
        increase: Boolean = false,
        depth: Boolean = true,
    ) {
        drawText(text, Vec3(x, y, z), scale, color, bgBox, increase, depth)
    }

    fun drawText(
        text: String,
        pos: Vec3,
        scale: Float = 1f,
        color: Int = 0xFFFFFFFF.toInt(),
        bgBox: Boolean = false,
        increase: Boolean = false,
        depth: Boolean = true
    ) {
        var finalScale = scale
        if (increase) {
            val dist = RenderQue.cam.position().distanceTo(pos)
            finalScale *= (dist.toFloat() / 120f) / 0.025f
        }

        val lines = text.split("\n")
        lines.forEachIndexed { i, line ->
            val yOffset = i * 9.0 * 0.025 * finalScale
            val linePos = pos.subtract(0.0, yOffset, 0.0)
            val bgColor = if (bgBox) (client.options.getBackgroundOpacity(0.25f) * 255).toInt() shl 24 else 0

            RenderQue.queueText(line, linePos, color, finalScale, shadow = true, depth = depth, bgColor = bgColor)
        }
    }
}