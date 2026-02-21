package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.config
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object leapAnnounce: Feature("leapAnnounce", island = SkyBlockIsland.THE_CATACOMBS) {
    private val leapRegex = "^You have teleported to (.+)".toRegex()
    private val message by config.property<String>("leapAnnounce.message")

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            val person = leapRegex.find(event.message.stripped)?.groups?.get(1)?.value ?: return@on
            ChatUtils.sendCommand("pc $message $person")
        }
    }
}