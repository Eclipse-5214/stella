package co.stellarskys.stella.events.core

import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DungeonFloor
import xyz.meowing.knit.api.events.Event

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
}