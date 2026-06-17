package co.stellarskys.stella.events.core

import com.mojang.blaze3d.vertex.PoseStack
import co.stellarskys.stella.api.events.Event
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.shapes.VoxelShape

sealed class RenderEvent {
    sealed class World {
        class Last(
            val matrices: PoseStack,
            val collector: SubmitNodeCollector
        ) : Event()

        class AfterEntities(
            val matrices: PoseStack,
            val collector: SubmitNodeCollector
        ) : Event()

        class BlockOutline(
            val matrices: PoseStack,
            val collector: SubmitNodeCollector,
            var blockPos: BlockPos,
            var voxelShape: VoxelShape
        ) : Event(cancelable = true)
    }
}