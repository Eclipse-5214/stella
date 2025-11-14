package co.stellarskys.stella.events.core

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.entity.state.PlayerRenderState
import xyz.meowing.knit.api.events.CancellableEvent
import xyz.meowing.knit.api.events.Event
import xyz.meowing.knit.api.render.world.RenderContext

sealed class RenderEvent {
    sealed class World {
        class Last(
            val context: RenderContext
        ) : Event()

        class AfterEntities(
            val context: RenderContext
        ) : Event()

        class BlockOutline(
            val context: RenderContext
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