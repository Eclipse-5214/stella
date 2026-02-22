package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object teammateMissing: Feature("teammateMissing", island = SkyBlockIsland.THE_CATACOMBS) {
    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            if(event.message.stripped != "Starting in 4 seconds.") return@on
            val players = Dungeon.players.count { it != null }
            if (players < 5) Utils.alert("§c$players/5 Players!", SoundEvents.NOTE_BLOCK_PLING.value())
        }
    }
}