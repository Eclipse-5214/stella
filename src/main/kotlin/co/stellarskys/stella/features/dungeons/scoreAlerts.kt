package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.Utils
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object scoreAlerts : Feature("scoreAlerts", island = SkyBlockIsland.THE_CATACOMBS) {
    private val alert270 by config.property<Boolean>("scoreAlerts.alert270")
    private val message270 by config.property<String>("scoreAlerts.message270")
    private val chat270 by config.property<Boolean>("scoreAlerts.chat270")
    private val chatMessage270 by config.property<String>("scoreAlerts.chatMessage270")

    private val alert300 by config.property<Boolean>("scoreAlerts.alert300")
    private val message300 by config.property<String>("scoreAlerts.message300")
    private val chat300 by config.property<Boolean>("scoreAlerts.chat300")
    private val chatMessage300 by config.property<String>("scoreAlerts.chatMessage300")

    private val alert5Crypts by config.property<Boolean>("scoreAlerts.alert5Crypts")
    private val message5Crypts by config.property<String>("scoreAlerts.message5Crypts")
    private val chat5Crypts by config.property<Boolean>("scoreAlerts.chat5Crypts")
    private val chatMessage5Crypts by config.property<String>("scoreAlerts.chatMessage5Crypts")

    override fun initialize() {
        on<DungeonEvent.Score.On270> {
            if (alert270) Utils.alert(message270.replace("&", "§"), SoundEvents.NOTE_BLOCK_PLING.value())
            if (chat270) ChatUtils.sendCommand("pc $chatMessage270")
        }

        on<DungeonEvent.Score.On300> {
            if (alert300) Utils.alert(message300.replace("&", "§"), SoundEvents.NOTE_BLOCK_PLING.value())
            if (chat300) ChatUtils.sendCommand("pc $chatMessage300")
        }
        on<DungeonEvent.Score.AllCrypts> {
            if (alert5Crypts) Utils.alert(message5Crypts.replace("&", "§"), SoundEvents.NOTE_BLOCK_PLING.value())
            if (chat5Crypts) ChatUtils.sendCommand("pc $chatMessage5Crypts")
        }
    }
}
