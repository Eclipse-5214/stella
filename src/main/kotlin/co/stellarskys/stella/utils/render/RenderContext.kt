package co.stellarskys.stella.utils.render

import com.mojang.blaze3d.vertex.PoseStack
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.Camera
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape
//#if MC < 1.21.9
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
//#else
//$$ import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext
//#endif

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
                //#if MC < 1.21.9
                matrixStack = ctx.matrixStack(),
                //#else
                //$$ matrixStack = ctx.matrices(),
                //#endif

                camera       = client.gameRenderer.mainCamera,
                consumers    = ctx.consumers(),
                blockPos     = null,
                voxelShape   = null
            )
        }
    }
}