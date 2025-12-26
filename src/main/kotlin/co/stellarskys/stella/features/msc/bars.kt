package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.vexel.Vexel
import co.stellarskys.vexel.api.nvg.NVGRenderer
import dev.deftu.omnicore.api.client.player
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.profile.StatsAPI
import java.awt.Color
import kotlin.math.max
import kotlin.math.min

@Module
object bars : Feature("bars", true) {
    private val HEALTH_REGEX = """(§.)(?<current>[\d,]+)/(?<max>[\d,]+)❤""".toRegex()
    private val MANA_REGEX = """§b(?<current>[\d,]+)/(?<max>[\d,]+)✎( Mana)?""".toRegex()
    private val OVERFLOW_REGEX  = """§3(?<overflowMana>[\d,]+)ʬ""".toRegex()

    val healthBar by config.property<Boolean>("bars.healthBar")
    val absorptionBar by config.property<Boolean>("bars.absorptionBar")
    val hpChange by config.property<Boolean>("bars.hpChange")
    val hpNum by config.property<Boolean>("bars.hpNum")

    val manaBar by config.property<Boolean>("bars.manaBar")
    val overflowManaBar by config.property<Boolean>("bars.overflowManaBar")
    val ofMana by config.property<Boolean>("bars.ofMana")
    val mpNum by config.property<Boolean>("bars.mpNum")

    // Hide vanilla UI
    val hideVanillaHealth by config.property<Boolean>("bars.hideVanillaHealth")
    val hideVanillaHunger by config.property<Boolean>("bars.hideVanillaHunger")
    val hideVanillaArmor by config.property<Boolean>("bars.hideVanillaArmor")

    // Colors
    val healthColor by config.property<RGBA>("bars.healthColor")
    val absorptionColor by config.property<RGBA>("bars.absorptionColor")

    val manaColor by config.property<RGBA>("bars.manaColor")
    val ofmColor by config.property<RGBA>("bars.ofmColor")

    private var lastHealth = StatsAPI.health.toFloat()
    private var healthDelta: Float? = null
    private var lastHealthDeltaTime = 0L


    val HPHudName = "hpHud"
    val HPChangeHudName = "hpChangeHud"
    val HPNumHudName = "hpNumHud"
    val MPHudName = "mpHud"
    val OFManaHudName = "ofManaHud"
    val MPNumHudName = "mpNumHud"

    val hpBarWidth get() = ((min(StatsAPI.health.toDouble(), StatsAPI.maxHealth.toDouble()) / StatsAPI.maxHealth.toDouble()) * 82.0).toFloat()
    val absBarWidth get() = ((max(StatsAPI.health.toDouble() - StatsAPI.maxHealth.toDouble(), 0.0) / StatsAPI.maxHealth.toDouble()) * 82.toDouble()).toFloat()
    val mpBarWidth get() = ((min(StatsAPI.mana.toDouble(), StatsAPI.maxMana.toDouble()) / StatsAPI.maxMana.toDouble()) * 82.0).toFloat()
    val ofBarWidth get() = (( StatsAPI.overflowMana.toDouble() / StatsAPI.maxMana.toDouble()) * 82.0).toFloat()


    private var smoothHp = 0f
    private var smoothAbs = 0f
    private var smoothMp = 0f
    private var smoothOf = 0f


    override fun initialize() {
        HUDManager.registerCustom(HPHudName, 90, 15, this::hpHudPreview, "bars.healthBar")
        HUDManager.registerCustom(HPNumHudName, 70,19, this::hpNumPreview,"bars.hpNum")
        HUDManager.registerCustom(HPChangeHudName, 30,19, this::hpChangePreview,"bars.hpChange")

        HUDManager.registerCustom(MPHudName, 90, 15, this::mpHudPreview, "bars.manaBar")
        HUDManager.registerCustom(MPNumHudName, 70,19, this::mpNumPreview,"bars.mpNum")
        HUDManager.registerCustom(OFManaHudName, 30,19, this::ofManaPreview,"bars.ofMana")


        on<GuiEvent.RenderHUD> {
            if (healthBar) hpHud(it.context)
            if (hpNum) hpNumHud(it.context)
            if (hpChange) { hpChangeHud(it.context); updateHealthDelta() }

            if (manaBar) mpHud(it.context)
            if (mpNum) mpNumHud(it.context)
            if (ofMana) ofManaHud(it.context)
        }
    }

    fun hpHudPreview(context: GuiGraphics) {
        context.drawNVG {
            Vexel.renderer.rect(5f, 5f, 80f, 5f, healthColor.toColorInt(), 3f)
        }
    }

    fun hpNumPreview(context: GuiGraphics) {
        val string = "1000/1000"
        val x = 35 - (string.width() / 2)
        Render2D.drawString(context, "1000", x,5, color = healthColor.toColor())
        Render2D.drawString(context, "§8/", x + "1000".width(), 5)
        Render2D.drawString(context, "1000", x + "1000/".width(), 5, color = healthColor.toColor())
    }

    fun hpChangePreview(context: GuiGraphics) {
        val string = "+123"
        val x = 15 - (string.width() / 2)
        Render2D.drawString(context, "§a$string", x,5)
    }

    fun mpHudPreview(context: GuiGraphics) {
        context.drawNVG {
            Vexel.renderer.rect(5f, 5f, 80f, 5f, manaColor.toColorInt(), 3f)
        }
    }

    fun mpNumPreview(context: GuiGraphics) {
        val string = "1000/1000"
        val x = 35 - (string.width() / 2)
        Render2D.drawString(context, "1000", x,5, color = manaColor.toColor())
        Render2D.drawString(context, "§8/", x + "1000".width(), 5)
        Render2D.drawString(context, "1000", x + "1000/".width(), 5, color = manaColor.toColor())
    }

    fun ofManaPreview(context: GuiGraphics) {
        val string = "400"
        val x = 15 - (string.width() / 2)
        Render2D.drawString(context, string + "ʬ", x,5, color = ofmColor.toColor())
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
                if(absorptionBar) Vexel.renderer.rect(-1f, 0f, smoothAbs, 5f, absorptionColor.toColorInt(), 3f)
            }
        }

        matrix.popMatrix()
    }

    fun hpNumHud(context: GuiGraphics) {
        val matrix = context.pose()

        val left = StatsAPI.health
        val slash = "/"
        val right = StatsAPI.maxHealth
        val width = left.toString().width() + slash.width() + right.toString().width()

        val x = HUDManager.getX(HPNumHudName)
        val y = HUDManager.getY(HPNumHudName)
        val scale = HUDManager.getScale(HPNumHudName)

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale)
        matrix.translate(35f - width / 2, 5f)

        val leftColor = if(left > right) absorptionColor else healthColor
        Render2D.drawString(context, left.toString(), 0, 0, color = leftColor.toColor())
        Render2D.drawString(context, "§8$slash", left.toString().width(), 0)
        Render2D.drawString(context, right.toString(), left.toString().width() + slash.width(), 0, color = healthColor.toColor())

        matrix.popMatrix()
    }

    fun hpChangeHud(context: GuiGraphics) {
        val matrix = context.pose()

        val x = HUDManager.getX(HPChangeHudName)
        val y = HUDManager.getY(HPChangeHudName)
        val scale = HUDManager.getScale(HPChangeHudName)

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale)
        matrix.translate(15f, 5f)

        healthDelta?.let { delta ->
            val text = if (delta > 0) "+${delta.toInt()}" else delta.toInt().toString()
            val color = if (delta > 0) "§a" else "§c"
            val width = text.width()
            matrix.translate(-width / 2f, 0f)
            Render2D.drawString(context,"$color$text",  0,0)
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
        matrix.translate(5f , 5f)

        context.drawNVG {
            NVGRenderer.drawMasked(0f, 0f, 80f, 5f, 3f) {
                Vexel.renderer.rect(0f, 0f, 80f, 5f, Color.BLACK.rgb)
                Vexel.renderer.rect(-1f, 0f, smoothMp, 5f, manaColor.toColorInt(), 3f)
                if(overflowManaBar) Vexel.renderer.rect(-1f, 0f, smoothOf, 5f, ofmColor.toColorInt(), 3f)
            }
        }

        matrix.popMatrix()
    }

    fun ofManaHud(context: GuiGraphics) {
        val matrix = context.pose()

        val x = HUDManager.getX(OFManaHudName)
        val y = HUDManager.getY(OFManaHudName)
        val scale = HUDManager.getScale(OFManaHudName)

        val string = StatsAPI.overflowMana.toString() + "ʬ"
        val width = string.width()

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale)
        matrix.translate(15f - width / 2, 5f)

        Render2D.drawString(context, string, 0,0, color = ofmColor.toColor())

        matrix.popMatrix()
    }

    fun mpNumHud(context: GuiGraphics) {
        val matrix = context.pose()

        val left = StatsAPI.mana
        val slash = "/"
        val right = StatsAPI.maxMana
        val width = left.toString().width() + slash.width() + right.toString().width()

        val x = HUDManager.getX(MPNumHudName)
        val y = HUDManager.getY(MPNumHudName)
        val scale = HUDManager.getScale(MPNumHudName)

        matrix.pushMatrix()
        matrix.translate(x, y)
        matrix.scale(scale)
        matrix.translate(35f - width / 2, 5f)

        Render2D.drawString(context, left.toString(), 0, 0, color = manaColor.toColor())
        Render2D.drawString(context, "§8$slash", left.toString().width(), 0)
        Render2D.drawString(context, right.toString(), left.toString().width() + slash.width(), 0, color = manaColor.toColor())

        matrix.popMatrix()
    }


    private fun lerp(current: Float, target: Float, speed: Float): Float {
        return current + (target - current) * speed
    }

    private fun updateHealthDelta() {
        val current = StatsAPI.health.toFloat()

        if (current != lastHealth) {
            healthDelta = current - lastHealth
            lastHealthDeltaTime = System.currentTimeMillis()
            lastHealth = current
        }

        if (healthDelta != null && System.currentTimeMillis() - lastHealthDeltaTime > 3000) {
            healthDelta = null
        }
    }

    fun cleanAB(text: Component): Component{
        if (!hpNum && !mpNum && !ofMana || !this.isEnabled()) return text
        val msg = text.string
        var cleaned = msg
        if (hpNum) cleaned = HEALTH_REGEX.replace(cleaned, "")
        if (mpNum) cleaned = MANA_REGEX.replace(cleaned, "")
        if (ofMana) cleaned = OVERFLOW_REGEX.replace(cleaned, "")
        cleaned = cleaned.trim().replace("  ", " ")
        println(cleaned)
        return if (cleaned != msg) Component.literal(cleaned) else text
    }
}
