package co.stellarskys.stella.events.core

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.state.PlayerRenderState
import co.stellarskys.stella.events.api.CancellableEvent
import co.stellarskys.stella.events.api.Event
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape

sealed class RenderEvent {
    sealed class World {
        class Last(
            val context: WorldRenderContext
        ) : Event()

        class AfterEntities(
            val context: WorldRenderContext
        ) : Event()

        class BlockOutline(
            val context: WorldRenderContext,
            val blockPos: BlockPos?,
            val blockShape: VoxelShape?,
        ) : CancellableEvent()
    }

    sealed class Entity {
        class Pre(
            val entity: net.minecraft.world.entity.Entity,
            val matrices: PoseStack,
            val vertex: MultiBufferSource?,
            val light: Int
        ) : CancellableEvent()

        class Post(
            val entity: net.minecraft.world.entity.Entity,
            val matrices: PoseStack,
            val vertex: MultiBufferSource?,
            val light: Int
        ) : Event()
    }

    sealed class Player {
        class Pre(
            val entity: PlayerRenderState,
            val matrices: PoseStack
        ) : CancellableEvent()
    }
}