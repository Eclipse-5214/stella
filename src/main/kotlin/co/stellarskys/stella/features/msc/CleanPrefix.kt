package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextUtils.substring

@Module
object CleanPrefix : Feature("cleanPrefix") {
    private val guildRegex = """^§2Guild > (?:.*?(§[0-9a-fk-or])\[[^]]+]\s+)?([a-zA-Z0-9_]+)(?: (§[0-9a-fk-or]\[[^]]+]))?§f: (.+)""".toRegex()
    private val partyRegex = """^§9Party §8> (?:.*?(§[0-9a-fk-or])\[[^]]+]\s+)?([a-zA-Z0-9_]+)§f: (.+)""".toRegex()

    override fun initialize() {
        on<ChatEvent.Modify.Receive>(-1) { event ->
            event matchesEntireRaw guildRegex modifyC { m ->
                val rankColor = m.groupValues[1].ifEmpty { "§7" }
                val guildTag = m.groupValues[3].takeIf { it.isNotEmpty() }?.let { " $it" } ?: ""
                Component.literal("§2G §8> $guildTag$rankColor${m.groupValues[2]}§f: ").append(event.usrMsg())
            }

            event matchesEntireRaw partyRegex modifyC { m ->
                val rankColor = m.groupValues[1].ifEmpty { "§7" }
                Component.literal("§9P §8> $rankColor${m.groupValues[2]}§f: ").append(event.usrMsg())
            }
        }
    }

    private fun ChatEvent.usrMsg(): Component = stripped.indexOf(": ").takeIf { it != -1 }?.let { message.substring(it + 2) } ?: Component.empty()
}