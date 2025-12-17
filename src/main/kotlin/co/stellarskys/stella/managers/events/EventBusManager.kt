package co.stellarskys.stella.managers.events

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.api.EventHandle
import co.stellarskys.stella.events.core.LocationEvent
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonAPI
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockArea
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland


@Module
object EventBusManager {
    private data class ConditionalEventHandle(
        val islands: Set<SkyBlockIsland>?,
        val arias: Set<SkyBlockArea>?,
        val floors: Set<DungeonFloor>?,
        val skyblockOnly: Boolean,
        val handle: EventHandle<*>,
        var isRegistered: Boolean = false
    )


    private val conditionalEventCalls = mutableListOf<ConditionalEventHandle>()

    init {
        EventBus.on<LocationEvent.IslandChange> { updateRegistrations() }
        EventBus.on<LocationEvent.ServerChange> { updateRegistrations() }
        EventBus.on<LocationEvent.AreaChange> { updateRegistrations() }
        EventBus.on<LocationEvent.DungeonFloorChange> { updateRegistrations() }
    }

    private fun updateRegistrations() {
        val currentIsland = LocationAPI.island
        val currentAria   = LocationAPI.area
        val currentFloor  = DungeonAPI.dungeonFloor
        val onSkyblock    = LocationAPI.isOnSkyBlock

        conditionalEventCalls.forEach { call ->
            val shouldBeRegistered = when {
                call.skyblockOnly && !onSkyblock -> false
                call.islands != null && currentIsland !in call.islands -> false
                call.arias   != null && currentAria   !in call.arias   -> false
                call.floors  != null && currentFloor  !in call.floors  -> false
                else -> true
            }

            if (shouldBeRegistered && !call.isRegistered) {
                call.handle.register()
                call.isRegistered = true
            } else if (!shouldBeRegistered && call.isRegistered) {
                call.handle.unregister()
                call.isRegistered = false
            }
        }
    }

    fun trackConditionalEvent(
        islands: Set<SkyBlockIsland>? = null,
        arias: Set<SkyBlockArea>? = null,
        floors: Set<DungeonFloor>? = null,
        skyblockOnly: Boolean = false,
        handle: EventHandle<*>
    ) {
        conditionalEventCalls.add(
            ConditionalEventHandle(islands, arias, floors, skyblockOnly, handle, false)
        )
        updateRegistrations()
    }
}
