package co.stellarskys.stella.utils.config.ui

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.*
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import co.stellarskys.stella.utils.config.ui.base.Panel
import co.stellarskys.stella.utils.config.ui.base.Subcategory
import co.stellarskys.stella.utils.config.ui.elements.*
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.stella.utils.render.nvg.Gradient
import co.stellarskys.stella.utils.render.nvg.NVGRenderer
import co.stellarskys.vexel.components.base.VexelElement
import com.mojang.blaze3d.opengl.GlTexture
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniMouse
import dev.deftu.omnicore.api.client.input.OmniMouseButton
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.client.screen.OmniScreen
import tech.thatgravyboat.skyblockapi.platform.texture
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import java.awt.Color
import kotlin.collections.component1
import kotlin.collections.component2

internal class ConfigUI(categories: Map<String, ConfigCategory>, config: Config): OmniScreen(dev.deftu.textile.Text.literal("Config")) {
    private val panels =  mutableListOf<Panel>()
    private val elementContainers = mutableMapOf<String, BaseElement>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private var needsVisibilityUpdate = false
    private val imageCacheMap = HashMap<String, Int>()
    private val revealDelegate = Utils.animate<Float>(0.15, AnimType.EASE_OUT)
    private var reveal by revealDelegate
    private var opening = true
    private val nvg get() = NVGRenderer
    private val rez get() = OmniResolution
    private val mouse = OmniMouse

    init {
        var sx = 10f
        categories.forEach { (title, category) ->
            val panel = buildCategory(sx, 50f, category, title, config)
            sx += panel.width + 10f
        }

        reveal = 0f
        revealDelegate.snap()
        reveal = rez.scaledWidth.toFloat()
    }

    override val isPausingScreen: Boolean = false

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        super.onRender(ctx, mouseX, mouseY, tickDelta)

        val context = ctx.graphics ?: return
        context.drawNVG {
            applyOpeningScissor()

            drawHeader()
            panels.forEach {
                it.render(context, mouseX.toFloat(), mouseY.toFloat(), tickDelta)
            }

            nvg.popScissor()
        }

        /*

        val size = (48 / OmniResolution.scaleFactor).toInt()
        val x = (head.raw.left / OmniResolution.scaleFactor).toInt()
        val y = (head.raw.top/ OmniResolution.scaleFactor).toInt()


        Render2D.drawPlayerHead(context, x, y, size, uuid)
         */
    }

    private fun buildCategory(x: Float, y: Float, category: ConfigCategory, title: String, config: Config): Panel {
        val panel = Panel(x, y, title)

        var sy = 20f
        category.subcategories.forEach { (key, subcategory) ->
            val sub = buildSubcategory(0f, sy, panel, subcategory, config)
            sy += sub.height
        }

        panel.update()
        panels.add(panel)
        return panel
    }

    private fun buildSubcategory(x: Float, y: Float, panel: Panel, subcategory: ConfigSubcategory, config: Config): Subcategory {
        val sub = Subcategory(x, y, subcategory)

        var ey = Subcategory.HEIGHT
        subcategory.elements.entries.forEachIndexed { index, (key, element) ->
            val component = when (element) {
                is Button -> ButtonUI(0f, ey,element)
                //is ColorPicker -> ColorPickerUIBuilder().build(box, element, window)
                //is Dropdown -> DropdownUIBuilder().build(box, element, window)
                //is Keybind -> KeybindUIBuilder().build(box, element, window)
                //is Slider -> SliderUIBuilder().build(box, element, window)
                //is StepSlider -> StepSliderUIBuilder().build(box, element, window)
                //is TextInput -> TextInputUIBuilder().build(box, element, window)
                //is TextParagraph -> TextParagraphUIBuilder().build(box, element)
                is Toggle -> ToggleUI(0f, ey, element)
                else -> null
            }

            if (component == null) return@forEachIndexed

            component.parent = sub
            sub.elements.add(component)

            elementContainers[element.configName] = component
            elementRefs[element.configName] = element

            needsVisibilityUpdate = true
            scheduleVisibilityUpdate(config)

            ey += component.height
        }

        sub.parent = panel
        sub.update()
        panel.elements.add(sub)
        return sub
    }

    fun updateUI(config: Config) {
        needsVisibilityUpdate = true
        scheduleVisibilityUpdate(config)
    }

    private fun scheduleVisibilityUpdate(config: Config) {
        if (!needsVisibilityUpdate) return

        elementContainers.keys.forEach { key ->
            updateElementVisibility(key, config)
        }

        needsVisibilityUpdate = false
    }

    private fun updateElementVisibility(configKey: String, config: Config) {
        val container = elementContainers[configKey] ?: return
        val element = elementRefs[configKey] ?: return
        val visible = element.isVisible(config)
        container.visible = visible
    }

    fun drawHeader() {
        val swx = rez.scaledWidth / 2
        nvg.push()
        nvg.translate(swx - 50f, 10f)
        nvg.rect(0f, 0f, 100f, 30f, Palette.Crust.rgb, 15f)
        drawPlayer(10f, 2.5f, 25f, 25f,  3f)
        nvg.text(player?.name?.stripped ?: "",40f, 5f, 10f, Palette.Text.rgb, nvg.inter)
        nvg.text("Stella User",40f, 17f, 8f, Palette.Subtext1.rgb, nvg.inter)
        nvg.hollowGradientRect(0f, 0f, 100f, 30f, 1f, Palette.Purple.rgb, Palette.Mauve.rgb, Gradient.TopLeftToBottomRight, 15f)
        nvg.pop()
    }

    fun drawPlayer(x: Float, y: Float, width: Float, height: Float, radius: Float) {
        val skin = player?.skin?.texture ?: return
        imageCacheMap.getOrPut(skin.path) {
            val texture = client.textureManager.getTexture(skin)?.texture as? GlTexture
            nvg.createNVGImage(texture?.glId() ?: 0, 64, 64)
        }.let { id ->
            nvg.image(id, 64, 64, 8, 8, 8, 8, x, y, width, height, radius)
            nvg.image(id, 64, 64, 40, 8, 8, 8, x, y, width, height, radius)
        }
    }

    private fun applyOpeningScissor() {
        if (opening && revealDelegate.done()) {
            opening = false
        }

        val sw = rez.scaledWidth.toFloat()
        val sh = rez.scaledHeight.toFloat()

        val halfW = reveal / 2f
        val cx = sw / 2f

        val x = cx - halfW
        val width = reveal

        // Full height, only X is animated
        nvg.pushScissor(x, 0f, width, sh)
    }


    override fun onMouseClick(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        for (panel in panels) panel.mouseClicked(x.toFloat(), y.toFloat(), button.code)
        return super.onMouseClick(button, x, y, modifiers)
    }
}