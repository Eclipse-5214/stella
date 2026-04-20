package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.utils.config
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object LeapAnnounce: Feature("leapAnnounce", island = SkyBlockIsland.THE_CATACOMBS) {
    private val leapRegex = "^You have teleported to (\\w+)".toRegex()
    private val message by config.property<String>("leapAnnounce.message")

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            val person = leapRegex.find(event.message.stripped)?.groups?.get(1)?.value ?: return@on
            val finalMessage = if (message.contains($$"$player")) message.replace($$"$player", person) else "$message $person"
            Signal.sendCommand("pc $finalMessage")
        }
    }
}