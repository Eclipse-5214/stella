package co.stellarskys.stella.utils.config.ui

import co.stellarskys.stella.utils.config.core.*
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.Panel
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
    private var subcatRefs = mutableListOf<VexelElement<*>>()
    private var needsVisibilityUpdate = false

    init {
        categories.forEach { title, catagory ->  }
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

    private fun buildCategory(x: Float, y: Float, category: ConfigCategory, title: String, config: Config) {
        val panel = Panel(x, y, title)
        // insert subcategory logic

        panel.update()
        panels.add(panel)
    }

    private fun buildSubcategory(root: VexelElement<*>, window: VexelWindow, subcategory: ConfigSubcategory, title: String, config: Config) {
        val box = Rectangle(Palette.Purple.withAlpha(20).rgb, Palette.Purple.withAlpha(100).rgb, 5f, 2f)
            .setSizing(90f, Size.Percent, 0f, Size.Auto)
            .setPositioning(0f, Pos.ParentCenter, 10f, Pos.AfterSibling)
            .childOf(root)

        subcatRefs += box

        val titlebox = Rectangle(Palette.Purple.withAlpha(100).rgb)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.ParentPixels)
            .borderRadiusVarying(5f,  5f, 0f, 0f)
            .childOf(box)

        val titleText = Text(title, shadowEnabled = false, fontSize = 14f)
            .setPositioning(5f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(titlebox)

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

        subcatRefs.forEach { element ->
            element.cache.sizeCacheValid = false
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