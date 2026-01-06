package co.stellarskys.stella.utils.config.ui

import co.stellarskys.stella.utils.config.core.*
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.Panel
import co.stellarskys.stella.utils.config.ui.base.Subcategory
import co.stellarskys.stella.utils.config.ui.elements.*
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelScreen
import co.stellarskys.vexel.core.VexelWindow
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.client.screen.OmniScreen
import java.awt.Color
import kotlin.collections.component1
import kotlin.collections.component2

internal class ConfigUI(categories: Map<String, ConfigCategory>, config: Config): OmniScreen(dev.deftu.textile.Text.literal("Config")) {
    private val panels =  mutableListOf<Panel>()
    private val elementContainers = mutableMapOf<String, VexelElement<*>>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private var needsVisibilityUpdate = false

    init {
        var sx = 10f
        categories.forEach { title, category ->
            val panel = buildCategory(sx, 10f, category, title, config)
            sx += panel.width + 10f
        }
    }

    override val isPausingScreen: Boolean = false

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        super.onRender(ctx, mouseX, mouseY, tickDelta)

        val context = ctx.graphics ?: return
        context.drawNVG {
            panels.forEach {
                it.render(context, mouseX.toFloat(), mouseY.toFloat(), tickDelta)
            }
        }

        /*
        val player = player ?: return
        val uuid = player.gameProfile.id
        val size = (48 / OmniResolution.scaleFactor).toInt()
        val x = (head.raw.left / OmniResolution.scaleFactor).toInt()
        val y = (head.raw.top/ OmniResolution.scaleFactor).toInt()


        Render2D.drawPlayerHead(context, x, y, size, uuid)
         */
    }

    private fun buildCategory(x: Float, y: Float, category: ConfigCategory, title: String, config: Config): Panel {
        val panel = Panel(x, y, title)

        var sy = 15f
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

        subcategory.elements.entries.forEachIndexed { index, (key, element) ->
            /*
            val component = when (element) {
                //is Button -> ButtonUIBuilder().build(box, element, window)
                //is ColorPicker -> ColorPickerUIBuilder().build(box, element, window)
                //is Dropdown -> DropdownUIBuilder().build(box, element, window)
                //is Keybind -> KeybindUIBuilder().build(box, element, window)
                //is Slider -> SliderUIBuilder().build(box, element, window)
                //is StepSlider -> StepSliderUIBuilder().build(box, element, window)
                //is TextInput -> TextInputUIBuilder().build(box, element, window)
                //is TextParagraph -> TextParagraphUIBuilder().build(box, element)
                //is Toggle -> ToggleUIBuilder().build(box, element, window)
                //else -> null
            }

            if (component == null) return@forEachIndexed

            elementContainers[element.configName] = component
            elementRefs[element.configName] = element

            needsVisibilityUpdate = true
            scheduleVisibilityUpdate(config)
             */
        }

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

        if (visible) container.show() else container.hide()

        container.cache.positionCacheValid = false
    }
}