package co.stellarskys.stella.events.core

import co.stellarskys.stella.utils.skyblock.dungeons.utils.DungeonFloor
import co.stellarskys.stella.utils.skyblock.location.SkyBlockArea
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import net.hypixel.data.region.Environment
import net.hypixel.data.type.ServerType
import co.stellarskys.stella.events.api.Event

sealed class LocationEvent {
    class ServerChange(
        val name: String,
        val type: ServerType?,
        val lobby: String?,
        val mode: String?,
        val map: String?,
    ) : Event()

    class IslandChange(
        val old: SkyBlockIsland?,
        val new: SkyBlockIsland?
    ) : Event()

    class AreaChange(
        val old: SkyBlockArea,
        val new: SkyBlockArea
    ) : Event()

    class DungeonFloorChange(
        val new: DungeonFloor?
    ) : Event()

    class HypixelJoin(
        val environment: Environment
    ) : Event() {
        val onAlpha: Boolean get() = environment == Environment.BETA
    }

    class SkyblockJoin : Event()

    class SkyblockLeave : Event()
}