package co.stellarskys.stella.utils.config.ui

import co.stellarskys.stella.utils.config.core.*
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.core.*
import co.stellarskys.stella.utils.config.ui.elements.ButtonUIBuilder
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.nvg.NVGRenderer
import co.stellarskys.stella.utils.render.nvg.NVGSpecialRenderer
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.client.screen.OmniScreen
import dev.deftu.textile.Text
import java.awt.Color
import kotlin.collections.component1
import kotlin.collections.component2

class NewConfigUI(categories: Map<String, ConfigCategory>, config: Config): OmniScreen(Text.literal("Config")) {
    private val elementContainers = mutableMapOf<String, ConfigInteractable>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private var subcatRefs = mutableListOf<ConfigSubcategoryUI>()
    private var needsVisibilityUpdate = false

    private var selectedCategory = categories.entries.firstOrNull()?.value ?: error("No categories found")
    private var displayedCategoryUI: ConfigCategoryUI = buildCategory(selectedCategory, config)

    private var background = Background(categories)

    class Background(val categories: Map<String, ConfigCategory>): ConfigElementUI() {
        init {
            width = sw / 2
            height = sh / 2
            x = scx
            y = scy
        }

        override fun render() {
            nvg.rect(x, y, width, height, Color.BLACK.rgb, 5f)
            nvg.hollowRect(x, y, width, height, 2f, Palette.Purple.withAlpha(100).rgb, 5f)

            nvg.text(player?.name?.string ?: "null", x + 70f, y + 12, 16f, Color.WHITE.rgb, nvg.defaultFont)
            nvg.text("Stella User", x + 70f, y + 30, 12f, Color.gray.rgb, nvg.defaultFont)

            var buttonY = sh * 0.3f + scy + 20f
            categories.entries.forEachIndexed { _, category ->
                nvg.rect(x + 10, y, width, height, if (category == selec), 5f)
            }
        }
    }

    override val isPausingScreen: Boolean = false

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        super.onRender(ctx, mouseX, mouseY, tickDelta)
        val context = ctx.graphics ?: return

        NVGSpecialRenderer.draw(context, 0, 0, context.guiWidth(), context.guiHeight()) {
            background.render()
            displayedCategoryUI.render()
        }

        val player = player ?: return
        val uuid = player.gameProfile.id
        val size = (48 / OmniResolution.scaleFactor).toInt()
        //val x = (head.raw.left / OmniResolution.scaleFactor).toInt()
        //val y = (head.raw.top/ OmniResolution.scaleFactor).toInt()


        //Render2D.drawPlayerHead(context, x, y, size, uuid)
    }

    private fun buildCategory(category: ConfigCategory, config: Config): ConfigCategoryUI {
        val categoryUI = ConfigCategoryUI()

        elementRefs.clear()
        elementContainers.clear()
        subcatRefs.clear()

        category.subcategories.entries.forEachIndexed { index, (name, subcategory) ->
            if (index % 2 == 0) {
                categoryUI.column1.add(buildSubcategory(subcategory, name, categoryUI,  config))
            } else {
                categoryUI.column2.add(buildSubcategory(subcategory, name, categoryUI, config))
            }
        }
        categoryUI.update()

        return categoryUI
    }

    private fun buildSubcategory(subcategory: ConfigSubcategory, title: String, category: ConfigCategoryUI, config: Config): ConfigSubcategoryUI {
        val subcategoryUI = ConfigSubcategoryUI(category)

        subcategory.elements.entries.forEachIndexed { index, (key, element) ->
            /*
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
             */

            val component = ConfigInteractable(subcategoryUI)

            if (component == null) return@forEachIndexed

            elementContainers[element.configName] = component
            elementRefs[element.configName] = element

            needsVisibilityUpdate = true
            scheduleVisibilityUpdate(config)
        }

        subcategoryUI.update()
        return subcategoryUI
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

        container.hidden = !visible
    }
}