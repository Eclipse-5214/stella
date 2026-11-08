package co.stellarskys.stella.api.dungeons.utils

import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.DungeonEvent
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland


object Phases {
    const val STORM = "[BOSS] Storm: Pathetic Maxor, just like expected."
    const val GOLDOR = "[BOSS] Goldor: Who dares trespass into my domain?"
    const val NECRON = "[BOSS] Necron: You went further than any human before, congratulations."
    const val KING = "[BOSS] The Wither King: You... again?"

    var currentPhase = F7.P1
        private set

    fun init() {
        EventBus.on<ChatEvent.Receive>(SkyBlockIsland.THE_CATACOMBS) { event ->
            if (Dungeon.floorNumber != 7) return@on

            when (event.stripped.trim()) {
                STORM -> {
                    currentPhase = F7.P2
                    EventBus.post(DungeonEvent.PhaseChange(currentPhase))
                }
                GOLDOR -> {
                    currentPhase = F7.P3
                    EventBus.post(DungeonEvent.PhaseChange(currentPhase))
                }
                NECRON -> {
                    currentPhase = F7.P4
                    EventBus.post(DungeonEvent.PhaseChange(currentPhase))
                }
                KING -> {
                    currentPhase = F7.P5
                    EventBus.post(DungeonEvent.PhaseChange(currentPhase))
                }
            }
        }
    }

    fun reset() {
        currentPhase = F7.P1
    }

    enum class F7 { P1, P2, P3, P4, P5; }
}