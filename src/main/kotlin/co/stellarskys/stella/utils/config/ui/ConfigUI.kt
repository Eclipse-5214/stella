package co.stellarskys.stella.utils.config.ui

import co.stellarskys.stella.utils.config.core.*
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.elements.*
import co.stellarskys.stella.utils.render.Render2D
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
import java.awt.Color
import kotlin.collections.component1
import kotlin.collections.component2

internal class ConfigUI(categories: Map<String, ConfigCategory>, config: Config): VexelScreen("Config") {
    private var selectedCategory = categories.entries.firstOrNull()?.value
    private val elementContainers = mutableMapOf<String, VexelElement<*>>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private var subcatRefs = mutableListOf<VexelElement<*>>()
    private var needsVisibilityUpdate = false

    val head = Rectangle(Color(0,0,0,0).rgb)

    init {
        val bg = Rectangle(Color.BLACK.rgb,Palette.Purple.withAlpha(100).rgb, 5f, 2f)
            .setSizing(50f, Size.Percent, 50f, Size.Percent)
            .setPositioning(Pos.ScreenCenter, Pos.ScreenCenter)
            .childOf(window)

        val list = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f, borderThickness = 0f)
            .setSizing(20f, Size.Percent, 100f, Size.Percent)
            .setPositioning(0f, Pos.ParentPixels,0f, Pos.ParentPixels)
            .childOf(bg)

        val card = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f, borderThickness = 0f)
            .setSizing(80f, Size.Percent, 100f, Size.Percent)
            .setPositioning(0f,Pos.AfterSibling,0f, Pos.ParentPixels)
            .scrollable( true)
            .scrollbarColor(Palette.Purple.withAlpha(100).rgb)
            .childOf(bg)

        val top = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f, borderThickness = 0f)
            .setSizing(100f, Size.Percent, 30f, Size.Percent)
            .setPositioning(0f, Pos.ParentPixels,0f, Pos.ParentPixels)
            .childOf(list)

        head
            .setSizing(48f, Size.Pixels, 48f, Size.Pixels)
            .setPositioning(10f, Pos.ParentPercent, 10f, Pos.ParentPercent)
            .childOf(top)

        val username = Text(player?.name?.string ?: "null", shadowEnabled = false, fontSize = 16f)
            .setPositioning(60f, Pos.ParentPixels, 2f, Pos.ParentPixels)
            .childOf(head)

        val tag = Text("Stella User", Color.gray.rgb, shadowEnabled = false, fontSize = 14f)
            .setPositioning(60f, Pos.ParentPixels, 20f, Pos.ParentPixels)
            .childOf(head)


        // === Category Button Panel ===

        val categoryLabels = mutableMapOf<ConfigCategory, VexelElement<co.stellarskys.vexel.elements.Button>>()


        categories.entries.forEachIndexed { _, category ->
            // Actual button surface
            val button = co.stellarskys.vexel.elements.Button(
                category.key,
                if (selectedCategory == category.value) Palette.Purple.rgb else Color.WHITE.rgb,
                backgroundColor = if (selectedCategory == category.value) Palette.Purple.withAlpha(50).rgb else Color(0,0, 0,0).rgb,
                borderRadius = 5f,
                borderThickness = 0f,
                fontSize = 16f
            )
                .setSizing(80f, Size.Percent, 8f, Size.Percent)
                .setPositioning(0f, Pos.ParentCenter,20f, Pos.AfterSibling)
                .childOf(list)

            categoryLabels[category.value] = button

            // Click handler to change category view
            button.onMouseClick { _->
                if (selectedCategory != category) {
                    selectedCategory = category.value

                    // Update label highlight colors
                    categoryLabels.forEach { (cat, btn) ->
                        btn as co.stellarskys.vexel.elements.Button
                        btn.textColor(if (cat == selectedCategory) Palette.Purple.rgb else Color.WHITE.rgb)
                        btn.backgroundColor( if (cat == selectedCategory) Palette.Purple.withAlpha(50).rgb else Color(0,0, 0,0).rgb)
                    }
                    // Destroy left over window ui
                    FloatingUIManager.clearAll()

                    // Swap out current category panel
                    card.children.toList().forEach { it.destroy() }

                    // Reset scroll
                    card.scrollOffset = 0f

                    buildCategory(card, window, category.value, config)
                }
                true
            }
        }

        buildCategory(card, window, selectedCategory!!, config)
    }

    override val isPausingScreen: Boolean = false

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        super.onRender(ctx, mouseX, mouseY, tickDelta)

        val player = player ?: return
        val uuid = player.gameProfile.id
        val size = (48 / OmniResolution.scaleFactor).toInt()
        val x = (head.raw.left / OmniResolution.scaleFactor).toInt()
        val y = (head.raw.top/ OmniResolution.scaleFactor).toInt()

        val context = ctx.graphics ?: return
        Render2D.drawPlayerHead(context, x, y, size, uuid)
    }

    private fun buildCategory(root: VexelElement<*>, window: VexelWindow, category: ConfigCategory, config: Config) {
        val column1 = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f)
            .setSizing(50f, Size.Percent, 0f, Size.Auto)
            .setPositioning(0f,Pos.ParentPixels,0f, Pos.ParentPixels)
            .childOf(root)

        val column2 = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f)
            .setSizing(50f, Size.Percent, 0f, Size.Auto)
            .setPositioning(0f,Pos.AfterSibling,0f, Pos.ParentPixels)
            .childOf(root)

        //spacers
        Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f)
            .setSizing(100f, Size.Percent, 20f, Size.Pixels)
            .setPositioning(Pos.ParentCenter, Pos.AfterSibling)
            .childOf(column1)

        Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f)
            .setSizing(100f, Size.Percent, 20f, Size.Pixels)
            .setPositioning(Pos.ParentCenter, Pos.AfterSibling)
            .childOf(column2)

        elementRefs.clear()
        elementContainers.clear()
        subcatRefs.clear()

        category.subcategories.entries.forEachIndexed { index, (name, subcategory) ->
            if (index % 2 == 0) {
                buildSubcategory(column1, window, subcategory, name, config)
            } else {
                buildSubcategory(column2, window, subcategory, name, config)
            }

        }

        // more spaces
        Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(Pos.ParentCenter, Pos.AfterSibling)
            .childOf(column1)

        Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(Pos.ParentCenter, Pos.AfterSibling)
            .childOf(column2)
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
            val component = when (element) {
                is Button -> ButtonUIBuilder().build(box, element, window)
                is ColorPicker -> ColorPickerUIBuilder().build(box, element, window)
                is Dropdown -> DropdownUIBuilder().build(box, element, window)
                is Keybind -> KeybindUIBuilder().build(box, element, window)
                is Slider -> SliderUIBuilder().build(box, element, window)
                is StepSlider -> StepSliderUIBuilder().build(box, element, window)
                is TextInput -> TextInputUIBuilder().build(box, element, window)
                is TextParagraph -> TextParagraphUIBuilder().build(box, element)
                is Toggle -> ToggleUIBuilder().build(box, element, window)
                else -> null
            }

            if (component == null) return@forEachIndexed

            elementContainers[element.configName] = component
            elementRefs[element.configName] = element

            needsVisibilityUpdate = true
            scheduleVisibilityUpdate(config)
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