package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.api.profile.StatsAPI
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@Module
object bars : Feature("bars", true) {
    val enabled by config.property<Boolean>("bars.enabled")
    val healthBar by config.property<Boolean>("bars.healthBar")
    val manaBar by config.property<Boolean>("bars.manaBar")

    // Hide vanilla UI
    val hideVanillaHealth by config.property<Boolean>("bars.hideVanillaHealth")
    val hideVanillaMana by config.property<Boolean>("bars.hideVanillaMana")

    // Colors
    val healthColor by config.property<RGBA>("bars.healthColor")
    val absorptionColor by config.property<RGBA>("bars.absorptionColor")
    val manaColor by config.property<RGBA>("bars.manaColor")

    val HPHudName = "hpHud"
    val MPHudName = "mpHud"

    val hpBarWidth get() = ((min(StatsAPI.health.toDouble(), StatsAPI.maxHealth.toDouble()) / StatsAPI.maxHealth.toDouble() ) * 80.0).toInt()
    val absBarWidth get() = ((max(StatsAPI.health.toDouble() - StatsAPI.maxHealth.toDouble(), 0.0) / StatsAPI.maxHealth.toDouble()) * 80.toDouble()).toInt()

    override fun initialize() {
        HUDManager.registerCustom(HPHudName, 90, 15, this::hpHudPreview)
        HUDManager.registerCustom(MPHudName, 90, 15, this::mpHudPreview)

        on<GuiEvent.RenderHUD> {
            if (healthBar) hpHud(it.context)
        }
    }

    fun hpHudPreview(context: GuiGraphics) {
        Render2D.drawRoundRect(context, 5, 5, 80, 5, 3, healthColor.toColor())
    }

    fun mpHudPreview(context: GuiGraphics) {
        Render2D.drawRoundRect(context, 5, 5, 80, 5, 3, manaColor.toColor())
    }

    fun hpHud(context: GuiGraphics) {
        val matrix = context.pose()

        val x = HUDManager.getX(HPHudName)
        val y = HUDManager.getY(HPHudName)
        val scale = HUDManager.getScale(HPHudName)

        matrix.pushMatrix()
        matrix.translate(x,y)
        matrix.scale(scale)
        matrix.translate(5f, 5f)

        Render2D.drawRoundRect(context, 0, 0, 80, 5, 3, Color.BLACK)
        Render2D.drawRoundRect(context, 0, 0, hpBarWidth, 5, 3, healthColor.toColor())
        Render2D.drawRoundRect(context, 0, 0, absBarWidth, 5, 3, absorptionColor.toColor())

        matrix.popMatrix()
    }
}
