package co.stellarskys.stella.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext

/**
 * Stable render context abstraction.
 * Wraps the core rendering state we need, so our code
 * doesn't depend directly on Fabric internals.
 */
data class RenderContext(
    val matrixStack: PoseStack?,
    val consumers: MultiBufferSource?,
    val camera: Camera,
    var blockPos: BlockPos? = null,
    var voxelShape: VoxelShape? = null
) {
    companion object {
        fun fromContext(ctx: WorldRenderContext): RenderContext {
            return RenderContext(
                matrixStack = ctx.matrices(),
                camera       = client.gameRenderer.mainCamera,
                consumers    = ctx.consumers(),
                blockPos     = null,
                voxelShape   = null
            )
        }
    }
}