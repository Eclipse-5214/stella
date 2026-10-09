package co.stellarskys.stella.features.dungeons.adg

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.events.core.TickEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.sounds.SoundEvents
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object HealerTT: Feature("healerTT", island = SkyBlockIsland.THE_CATACOMBS) {
    const val STORM_START = "[BOSS] Storm: Pathetic Maxor, just like expected."

    val leapThreshold by config.property<Int>("healerTT.leap")
    val leapMessage by config.property<String>("healerTT.leapMessage")
    val padThreshold by config.property<Int>("healerTT.pad")
    val padMessage by config.property<String>("healerTT.padMessage")

    val hud by config.property<Boolean>("healerTT.hud")
    val inTicks by config.property<Boolean>("healerTT.inTicks")

    var startedStorm = false
    var sentLeap = false
    var sentPad = false
    var totalTicks = 0

    override fun initialize() {
        HUDManager.register("healerTT", "§a30.00", "healerTT.hud", setOf("healerTT.inTicks"))

        on<TickEvent.Server> {
            if (!startedStorm || totalTicks > 600) return@on
            totalTicks++

            if (totalTicks > leapThreshold && !sentLeap) {
                Utils.alert(leapMessage.replace("&", "§"), SoundEvents.NOTE_BLOCK_PLING.value())
                sentLeap = true
            }

            if (totalTicks > padThreshold && !sentPad) {
                Utils.alert(padMessage.replace("&", "§"), SoundEvents.NOTE_BLOCK_PLING.value())
                sentPad = true
            }
        }

        on<ChatEvent.Receive> { event ->
            if (event.stripped != STORM_START) return@on
            startedStorm = true
        }

        on<GuiEvent.RenderHUD>{ if (hud) renderHUD(it.context) }
        on<LocationEvent.IslandChange> { reset() }
    }

    override fun onRegister() { reset() }

    private fun renderHUD(context: GuiGraphicsExtractor) = HUDManager.renderHud("healerTT", context) {
        if (!startedStorm || totalTicks > padThreshold) return@renderHud
        val displayTicks = padThreshold - totalTicks

        val prefix = when  {
            totalTicks < 0.50 * leapThreshold -> "§a"
            totalTicks < 0.75 * leapThreshold -> "§e"
            else -> "§c"
        }

        val time = if (inTicks) displayTicks.toString() else "%.2f".format(displayTicks / 20.0)

        Render2D.drawString(context, "$prefix$time", 0, 0, shadow = false)
    }

    private fun reset() {
        startedStorm = false
        sentLeap = false
        sentPad = false
        totalTicks = 0
    }
}