package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.vexel.Vexel
import co.stellarskys.vexel.api.nvg.NVGRenderer
import net.minecraft.client.gui.GuiGraphics
import tech.thatgravyboat.skyblockapi.api.profile.StatsAPI
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@Module
object bars : Feature("bars", true) {
    val healthBar by config.property<Boolean>("bars.healthBar")
    val manaBar by config.property<Boolean>("bars.manaBar")

    // Hide vanilla UI
    val hideVanillaHealth by config.property<Boolean>("bars.hideVanillaHealth")
    val hideVanillaMana by config.property<Boolean>("bars.hideVanillaMana")

    // Colors
    val healthColor by config.property<RGBA>("bars.healthColor")
    val absorptionColor by config.property<RGBA>("bars.absorptionColor")
    val manaColor by config.property<RGBA>("bars.manaColor")
    val ofmColor = Palette.Purple.rgb

    val HPHudName = "hpHud"
    val MPHudName = "mpHud"

    val hpBarWidth get() = ((min(StatsAPI.health.toDouble(), StatsAPI.maxHealth.toDouble()) / StatsAPI.maxHealth.toDouble()) * 82.0).toFloat()
    val absBarWidth get() = ((max(StatsAPI.health.toDouble() - StatsAPI.maxHealth.toDouble(), 0.0) / StatsAPI.maxHealth.toDouble()) * 82.toDouble()).toFloat()
    val mpBarWidth get() = ((min(StatsAPI.mana.toDouble(), StatsAPI.maxMana.toDouble()) / StatsAPI.maxMana.toDouble()) * 82.0).toFloat()
    val ofBarWidth get() = (( StatsAPI.overflowMana.toDouble() / StatsAPI.maxMana.toDouble()) * 82.0).toFloat()


    private var smoothHp = 0f
    private var smoothAbs = 0f
    private var smoothMp = 0f
    private var smoothOf = 0f


    override fun initialize() {
        HUDManager.registerCustom(HPHudName, 90, 15, this::hpHudPreview)
        HUDManager.registerCustom(MPHudName, 90, 15, this::mpHudPreview)

        on<GuiEvent.RenderHUD> {
            if (healthBar) hpHud(it.context)
            if (manaBar) mpHud(it.context)
        }
    }

    fun hpHudPreview(context: GuiGraphics) {
        //Render2D.drawRoundRect(context, 5, 5, 80, 5, 3, healthColor.toColor())
    }

    fun mpHudPreview(context: GuiGraphics) {
        //Render2D.drawRoundRect(context, 5, 5, 80, 5, 3, manaColor.toColor())
    }

    fun hpHud(context: GuiGraphics) {
        val matrix = context.pose()

        val x = HUDManager.getX(HPHudName)
        val y = HUDManager.getY(HPHudName)
        val scale = HUDManager.getScale(HPHudName)

        smoothHp = lerp(smoothHp, hpBarWidth, 0.15f)
        smoothAbs = lerp(smoothAbs, absBarWidth, 0.15f)

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale)
        matrix.translate(5f, 5f)

        context.drawNVG {
            NVGRenderer.drawMasked(0f, 0f, 80f, 5f, 3f) {
                Vexel.renderer.rect(0f, 0f, 80f, 5f, Color.BLACK.rgb)
                Vexel.renderer.rect(-1f, 0f, smoothHp, 5f, healthColor.toColorInt(), 3f)
                Vexel.renderer.rect(-1f, 0f, smoothAbs, 5f, absorptionColor.toColorInt(), 3f)
            }
        }

        matrix.popMatrix()
    }


    fun mpHud(context: GuiGraphics) {
        val matrix = context.pose()

        val x = HUDManager.getX(MPHudName)
        val y = HUDManager.getY(MPHudName)
        val scale = HUDManager.getScale(MPHudName)

        smoothMp = lerp(smoothMp, mpBarWidth, 0.15f)
        smoothOf = lerp(smoothOf, ofBarWidth, 0.15f)

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale)
        matrix.translate(5f, 5f)

        context.drawNVG {
            NVGRenderer.drawMasked(0f, 0f, 80f, 5f, 3f) {
                Vexel.renderer.rect(0f, 0f, 80f, 5f, Color.BLACK.rgb)
                Vexel.renderer.rect(-1f, 0f, smoothMp, 5f, manaColor.toColorInt(), 3f)
                Vexel.renderer.rect(-1f, 0f, smoothOf, 5f, ofmColor, 3f)
            }
        }

        matrix.popMatrix()
    }


    private fun lerp(current: Float, target: Float, speed: Float): Float {
        return current + (target - current) * speed
    }
}
