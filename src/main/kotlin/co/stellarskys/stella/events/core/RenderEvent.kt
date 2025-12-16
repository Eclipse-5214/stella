package co.stellarskys.stella.events.core

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import co.stellarskys.stella.events.api.CancellableEvent
import co.stellarskys.stella.events.api.Event
import co.stellarskys.stella.utils.render.RenderContext

//?if >= 1.21.9 {
// import net.minecraft.client.renderer.entity.state.AvatarRenderState
//?} else {
import net.minecraft.client.renderer.entity.state.PlayerRenderState
//?}

sealed class RenderEvent {
    sealed class World {
        class Last(
            val context: RenderContext
        ) : Event()

        class AfterEntities(
            val context: RenderContext
        ) : Event()

        class BlockOutline(
            val context: RenderContext,
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
            //?if >= 1.21.9 {
            // val entity: AvatarRenderState,
            //?} else {
            val entity: PlayerRenderState,
            //?}

            val matrices: PoseStack
        ) : CancellableEvent()
    }
}