package co.stellarskys.stella.features.msc

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.GuiEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.hud.HUDManager
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.TimeUtils.millis
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.render.nvg.NVGRenderer
import co.stellarskys.vexel.Vexel
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.api.profile.StatsAPI
import java.awt.Color
import kotlin.math.max

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
    val healthColor by config.property<Color>("bars.healthColor")
    val absorptionColor by config.property<Color>("bars.absorptionColor")

    val manaColor by config.property<Color>("bars.manaColor")
    val ofmColor by config.property<Color>("bars.ofmColor")

    private var lastHealth = StatsAPI.health.toFloat()
    private var healthDelta: Float? = null
    private var lastHealthDeltaTime = TimeUtils.zero


    val HPHudName = "hpHud"
    val HPChangeHudName = "hpChangeHud"
    val HPNumHudName = "hpNumHud"
    val MPHudName = "mpHud"
    val OFManaHudName = "ofManaHud"
    val MPNumHudName = "mpNumHud"

    val hpBarWidth get() = ratioWidth(StatsAPI.health, StatsAPI.maxHealth)
    val absBarWidth get() = ratioWidth(max(StatsAPI.health.toDouble() - StatsAPI.maxHealth.toDouble(), 0.0), StatsAPI.maxHealth)
    val mpBarWidth get() = ratioWidth(StatsAPI.mana, StatsAPI.maxMana)
    val ofBarWidth get() = ratioWidth(StatsAPI.overflowMana, StatsAPI.maxMana)

    private var smoothHp by Utils.lerped<Float>(0.15)
    private var smoothAbs by Utils.lerped<Float>(0.15)
    private var smoothMp by Utils.lerped<Float>(0.15)
    private var smoothOf by Utils.lerped<Float>(0.15)

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

    fun hpHudPreview(context: GuiGraphics) = context.drawNVG {
        NVGRenderer.rect(5f, 5f, 80f, 5f, healthColor.rgb, 3f)
    }


    fun hpNumPreview(context: GuiGraphics) {
        val string = "1000/1000"
        val x = 35 - (string.width() / 2)
        Render2D.drawString(context, "1000", x,5, color = healthColor)
        Render2D.drawString(context, "§8/", x + "1000".width(), 5)
        Render2D.drawString(context, "1000", x + "1000/".width(), 5, color = healthColor)
    }

    fun hpChangePreview(context: GuiGraphics) {
        val string = "+123"
        val x = 15 - (string.width() / 2)
        Render2D.drawString(context, "§a$string", x,5)
    }

    fun mpHudPreview(context: GuiGraphics) = context.drawNVG {
            NVGRenderer.rect(5f, 5f, 80f, 5f, manaColor.rgb, 3f)
    }

    fun mpNumPreview(context: GuiGraphics) {
        val string = "1000/1000"
        val x = 35 - (string.width() / 2)
        Render2D.drawString(context, "1000", x,5, color = manaColor)
        Render2D.drawString(context, "§8/", x + "1000".width(), 5)
        Render2D.drawString(context, "1000", x + "1000/".width(), 5, color = manaColor)
    }

    fun ofManaPreview(context: GuiGraphics) {
        val string = "400"
        val x = 15 - (string.width() / 2)
        Render2D.drawString(context, string + "ʬ", x,5, color = ofmColor)
    }

    fun hpHud(context: GuiGraphics) = HUDManager.renderHud(HPHudName, context) {
        val matrix = context.pose()
        matrix.translate(5f, 5f)

        smoothHp = hpBarWidth
        smoothAbs = absBarWidth

        drawBar(context, smoothHp, smoothAbs, absorptionBar, healthColor, absorptionColor)
    }

    fun hpNumHud(context: GuiGraphics) = HUDManager.renderHud(HPNumHudName, context) {
        val matrix = context.pose()

        val left = StatsAPI.health
        val right = StatsAPI.maxHealth
        val text = "$left/$right"

        matrix.translate(35f - text.width() / 2, 5f)

        val leftColor = if (left > right) absorptionColor else healthColor
        Render2D.drawString(context, left.toString(), 0, 0, color = leftColor)
        Render2D.drawString(context, "§8/", left.toString().width(), 0)
        Render2D.drawString(context, right.toString(), "$left/".width(), 0, color = healthColor)
    }

    fun hpChangeHud(context: GuiGraphics) = HUDManager.renderHud(HPChangeHudName, context) {
        val matrix = context.pose()
        matrix.translate(15f, 5f)

        healthDelta?.let { delta ->
            val text = if (delta > 0) "+${delta.toInt()}" else delta.toInt().toString()
            val color = if (delta > 0) "§a" else "§c"
            val width = text.width()
            matrix.translate(-width / 2f, 0f)
            Render2D.drawString(context,"$color$text",  0,0)
        }
    }

    fun mpHud(context: GuiGraphics) = HUDManager.renderHud(MPHudName, context) {
        val matrix = context.pose()
        matrix.translate(5f, 5f)

        smoothMp = mpBarWidth
        smoothOf = ofBarWidth

        drawBar(context, smoothMp, smoothOf, overflowManaBar, manaColor, ofmColor)
    }

    fun ofManaHud(context: GuiGraphics) = HUDManager.renderHud(OFManaHudName, context){
        val matrix = context.pose()

        val string = StatsAPI.overflowMana.toString() + "ʬ"
        val width = string.width()

        matrix.translate(15f - width / 2, 5f)
        Render2D.drawString(context, string, 0,0, color = ofmColor)
    }

    fun mpNumHud(context: GuiGraphics) = HUDManager.renderHud(MPNumHudName, context) {
        val matrix = context.pose()

        val left = StatsAPI.mana
        val right = StatsAPI.maxMana
        val text = "$left/$right"

        matrix.translate(35f - text.width() / 2, 5f)

        Render2D.drawString(context, left.toString(), 0, 0, color = manaColor)
        Render2D.drawString(context, "§8/", left.toString().width(), 0)
        Render2D.drawString(context, right.toString(), "$left/".width(), 0, color = manaColor)
    }

    private fun updateHealthDelta() {
        val current = StatsAPI.health.toFloat()

        if (current != lastHealth) {
            healthDelta = current - lastHealth
            lastHealthDeltaTime = TimeUtils.now
            lastHealth = current
        }

        if (healthDelta != null && lastHealthDeltaTime.since.millis > 3000) {
            healthDelta = null
        }
    }

    private fun drawBar(context: GuiGraphics, mainWidth: Float, secondaryWidth: Float, showSecondary: Boolean, mainColor: Color, secondaryColor: Color) {
        context.drawNVG {
            NVGRenderer.drawMasked(0f, 0f, 80f, 5f, 3f) {
                NVGRenderer.rect(0f, 0f, 80f, 5f, Color.BLACK.rgb)
                NVGRenderer.rect(-1f, 0f, mainWidth, 5f, mainColor.rgb, 3f)
                if (showSecondary) {
                    NVGRenderer.rect(-1f, 0f, secondaryWidth, 5f, secondaryColor.rgb, 3f)
                }
            }
        }
    }

    private fun ratioWidth(current: Number, max: Number, full: Float = 82f): Float {
        val c = current.toDouble()
        val m = max.toDouble()
        return ((c / m).coerceIn(0.0, 1.0) * full).toFloat()
    }

    fun cleanAB(text: Component): Component {
        if (!isEnabled() || (!hpNum && !mpNum && !ofMana)) return text

        val msg = text.string
        val cleaned = msg.let {
            var t = it
            if (hpNum) t = HEALTH_REGEX.replace(t, "")
            if (mpNum) t = MANA_REGEX.replace(t, "")
            if (ofMana) t = OVERFLOW_REGEX.replace(t, "")
            t
        }.trim().replace("\\s+".toRegex(), " ")

        return if (cleaned != msg) Component.literal(cleaned) else text
    }
}
