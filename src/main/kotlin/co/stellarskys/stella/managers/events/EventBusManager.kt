package co.stellarskys.stella.managers.events

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.utils.skyblock.location.LocationAPI
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import xyz.meowing.knit.api.events.EventCall

@Module
object EventBusManager {
    private data class ConditionalEventCall(
        val islands: Set<SkyBlockIsland>?,
        val skyblockOnly: Boolean,
        val eventCall: EventCall,
        var isRegistered: Boolean = false
    )

    private val conditionalEventCalls = mutableListOf<ConditionalEventCall>()

    init {
        EventBus.register<LocationEvent.IslandChange> {
            updateRegistrations()
        }

        EventBus.register<LocationEvent.ServerChange> {
            updateRegistrations()
        }
    }

    private fun updateRegistrations() {
        val currentIsland = LocationAPI.island
        val onSkyblock = LocationAPI.isOnSkyBlock

        conditionalEventCalls.forEach { call ->
            val shouldBeRegistered = when {
                call.skyblockOnly && !onSkyblock -> false
                call.islands != null -> currentIsland in call.islands
                else -> true
            }

            if (shouldBeRegistered && !call.isRegistered) {
                call.eventCall.register()
                call.isRegistered = true
            } else if (!shouldBeRegistered && call.isRegistered) {
                call.eventCall.unregister()
                call.isRegistered = false
            }
        }
    }

    fun trackConditionalEvent(islands: Set<SkyBlockIsland>?, skyblockOnly: Boolean, eventCall: EventCall) {
        conditionalEventCalls.add(ConditionalEventCall(islands, skyblockOnly, eventCall, false))
        updateRegistrations()
    }
}

