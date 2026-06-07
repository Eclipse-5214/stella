package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature

@Module
object CleanPrefix: Feature("cleanPrefix") {
    private val guildRegex = "Guild > (\\[.+?])? ?([a-zA-Z0-9_]+) ?(\\[.+?])?: (.+)".toRegex()
    private val partyRegex = "Party > (\\[.+?])? ?(.+?): (.+)".toRegex()
    private val rankRegex = """\[(.+?)]""".toRegex()

    override fun initialize() {
        on<ChatEvent.Receive>(-1){ event ->
            event matches guildRegex modify { m ->
                val hRank = m.groupValues[1].color()
                val gRank = m.groupValues[3].takeIf { it.isNotEmpty() }?.let { "§8$it " } ?: ""
                "§2G §8> $gRank§$hRank${m.groupValues[2]}§f: ${m.groupValues[4]}"
            }

            event matches partyRegex modify { m ->
                val hRank = m.groupValues[1].color()
                "§9P §8> §$hRank${m.groupValues[2]}§f: ${m.groupValues[3]}"
            }
        }
    }

    private fun String.color(): String {
        if (this.isEmpty()) return "7"
        return when ((rankRegex.find(this)?.groupValues?.get(1))) {
            "MVP++" -> "6"
            "MVP+", "MVP" -> "b"
            "VIP+", "VIP" -> "a"
            else -> "c"
        }
    }
}