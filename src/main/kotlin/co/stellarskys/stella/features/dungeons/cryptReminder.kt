package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.score.DungeonScore
import dev.deftu.omnicore.api.scheduling.TickSchedulers
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.*
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

@Module
object cryptReminder: Feature("cryptReminder", island = SkyBlockIsland.THE_CATACOMBS) {
    val delay by config.property<Int>("cryptReminder.delay")
    var party by config.property<Boolean>("cryptReminder.sendParty")

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            if (event.message.stripped != "[NPC] Mort: Good luck.") return@on
            TickSchedulers.client.after(60 * 20 * delay) {
                val crypts = DungeonScore.data.crypts
                if (crypts > 4 || LocationAPI.island != SkyBlockIsland.THE_CATACOMBS || Dungeon.inBoss) return@after
                Utils.alert("§dCrypts: §c$crypts§7/§c5", SoundEvents.NOTE_BLOCK_PLING.value())
                if (party) ChatUtils.sendCommand("pc $crypts/5 crypts")
            }
        }
    }
}