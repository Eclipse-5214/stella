package co.stellarskys.stella.events.core

import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import co.stellarskys.stella.events.api.Event
import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonKey

sealed class DungeonEvent {
    class Start(
        val floor: DungeonFloor
    ) : Event()

    class End(
        val floor: DungeonFloor
    ) : Event()

    class Enter(
        val floor: DungeonFloor
    ) : Event()

    sealed class Player {
        class Death(
            val player: DungeonPlayer
        ) : Event()
    }

    sealed class Room {
        class Change(
            val old: co.stellarskys.stella.utils.skyblock.dungeons.map.Room,
            val new: co.stellarskys.stella.utils.skyblock.dungeons.map.Room
        ) : Event()

        class StateChange(
            val room: co.stellarskys.stella.utils.skyblock.dungeons.map.Room,
            val oldState: Checkmark,
            val newState: Checkmark,
            val roomPlayers: List<DungeonPlayer>
        ) : Event()
    }

    class KeyPickUp(
        val key: DungeonKey
    ) : Event()


    sealed class Secrets {
        class Item(
            val entityId: Int,
            val entity: Entity
        ) : Event()

        class Bat(
            val blockPos: BlockPos
        ) : Event()

        class Chest(
            val blockState: BlockState,
            val blockPos: BlockPos
        ) : Event()

        class Essence(
            val blockEntity: BlockEntity,
            val blockPos: BlockPos,
        ) : Event()

        class Misc(
            val secretType: Type,
            val blockPos: BlockPos
        ) : Event()

        enum class Type {
            RED_SKULL,
            LEVER
        }
    }
}