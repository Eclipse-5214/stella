package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.score.DungeonScore
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.events.core.LocationEvent
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.*
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.time.Duration.Companion.minutes

@Module
object CryptReminder: Feature("cryptReminder", island = SkyBlockIsland.THE_CATACOMBS) {
    private val delay by config.property<Int>("cryptReminder.delay")
    private val party by config.property<Boolean>("cryptReminder.sendParty")

    private var reminderHandle: Chronos.Task? = null
    private val crypts get() = DungeonScore.data.crypts

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            if (event.message.stripped != "[NPC] Mort: Good luck.") return@on
            reminderHandle?.cancel()
            reminderHandle = Chronos.Async after delay.minutes given { crypts < 5 && LocationAPI.island == SkyBlockIsland.THE_CATACOMBS && !Dungeon.inBoss } run {
                Utils.alert("§dCrypts: §c$crypts§7/§c5", SoundEvents.NOTE_BLOCK_PLING.value())
                if (party) Signal.sendCommand("pc $crypts/5 crypts")
            }
        }

        on<LocationEvent.ServerChange> {
            reminderHandle?.cancel()
            reminderHandle = null
        }
    }
}