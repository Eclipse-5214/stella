package co.stellarskys.stella.events.core

import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.renderer.MultiBufferSource
import co.stellarskys.stella.events.api.Event
import co.stellarskys.stella.utils.render.RenderContext
import net.minecraft.client.renderer.entity.state.AvatarRenderState
import net.minecraft.client.renderer.entity.state.EntityRenderState
import net.minecraft.network.chat.Component

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
        ) : Event(cancelable = true)
    }

    sealed class Entity {
        class Pre(
            val entity: net.minecraft.world.entity.Entity,
            val matrices: PoseStack,
            val vertex: MultiBufferSource?,
            val light: Int
        ) : Event(cancelable = true)
        class Post(
            val entity: net.minecraft.world.entity.Entity,
            val matrices: PoseStack,
            val vertex: MultiBufferSource?,
            val light: Int
        ) : Event()
        class Nametag(
            val state: EntityRenderState,
            val matrices: PoseStack,
            val vertex: MultiBufferSource?,
            val text: Component,
            val light: Int
        ) : Event(cancelable = true)
    }

    sealed class Player {
        class Pre(
            val entity: AvatarRenderState,
            val matrices: PoseStack
        ) : Event(cancelable = true)
    }
}