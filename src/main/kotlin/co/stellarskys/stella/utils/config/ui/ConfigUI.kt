package co.stellarskys.stella.utils.config.ui

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.*
import co.stellarskys.stella.utils.config.ui.base.*
import co.stellarskys.stella.utils.config.ui.elements.*
import co.stellarskys.stella.utils.render.nvg.Gradient
import co.stellarskys.stella.utils.render.nvg.NVGRenderer
import co.stellarskys.stella.utils.render.nvg.NVGSpecialRenderer
import com.mojang.blaze3d.opengl.GlTexture
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.input.*
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.client.screen.KeyPressEvent
import dev.deftu.omnicore.api.client.screen.OmniScreen
import tech.thatgravyboat.skyblockapi.platform.texture
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

internal class ConfigUI(categories: Map<String, ConfigCategory>, config: Config): OmniScreen(dev.deftu.textile.Text.literal("Config")) {
    private val panels =  mutableListOf<Panel>()
    private val elementContainers = mutableMapOf<String, BaseElement>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private var needsVisibilityUpdate = false
    private val imageCacheMap = HashMap<String, Int>()
    private val revealDelegate = Utils.animate<Float>(0.0375, AnimType.EASE_OUT)
    private var reveal by revealDelegate
    private var opening = true
    private val nvg get() = NVGRenderer
    private val rez get() = OmniResolution
    private val mouse = OmniMouse
    private val mx get() = mouse.rawX.toFloat() / UI_SCALE
    private val my get() = mouse.rawY.toFloat() / UI_SCALE

    init {
        var sx = 20f
        categories.forEach { (title, category) ->
            val panel = buildCategory(sx, 100f, category, title, config)
            sx += panel.width + 20f
        }
    }

    override val isPausingScreen: Boolean = false

    override fun onInitialize(width: Int, height: Int) {
        reveal = 0f
        revealDelegate.snap()
        reveal = rez.windowWidth.toFloat()
        super.onInitialize(width, height)
    }

    override fun onResize(width: Int, height: Int) {
        reveal = rez.windowWidth.toFloat()
        revealDelegate.snap()
        super.onResize(width, height)
    }

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        super.onRender(ctx, mouseX, mouseY, tickDelta)

        val context = ctx.graphics ?: return
        NVGSpecialRenderer.draw(context, 0, 0, context.guiWidth(), context.guiHeight()) {
            nvg.push()
            applyOpeningScissor()
            nvg.scale(UI_SCALE, UI_SCALE)

            drawHeader()
            panels.forEach {
                it.render(context, mx, my, tickDelta)
            }

            nvg.popScissor()
            nvg.pop()
        }
    }

    private fun buildCategory(x: Float, y: Float, category: ConfigCategory, title: String, config: Config): Panel {
        val panel = Panel(x, y, title)

        var sy = 40f
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
                is ColorPicker -> ColorPickerUI(0f, ey, element)
                is Dropdown -> DropdownUI(0f, ey, element)
                is Keybind -> KeybindUI(0f, ey, element)
                is Slider -> SliderUI(0f, ey, element)
                is StepSlider -> StepSliderUI(0f, ey, element)
                is TextInput -> TextInputUI(0f, ey, element)
                is TextParagraph -> TextParagraphUI(0f, ey, element)
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

        for (panel in panels) {
            panel.update()
            for(element in panel.elements) (element as? ParentElement)?.update()
        }

        needsVisibilityUpdate = false
    }

    private fun updateElementVisibility(configKey: String, config: Config) {
        val container = elementContainers[configKey] ?: return
        val element = elementRefs[configKey] ?: return
        val visible = element.isVisible(config)
        container.setVisibility(visible)
    }

    fun drawHeader() {
        val swx = (rez.windowWidth / UI_SCALE) / 2
        nvg.push()
        nvg.translate(swx - 100f, 20f)
        nvg.rect(0f, 0f, 200f, 60f, Palette.Crust.rgb, 30f)
        drawPlayer(20f, 5f, 50f, 50f,  6f)
        nvg.text(player?.name?.stripped ?: "",80f, 10f, 20f, Palette.Text.rgb, nvg.inter)
        nvg.text("Stella User",80f, 34f, 16f, Palette.Subtext1.rgb, nvg.inter)
        nvg.hollowGradientRect(0f, 0f, 200f, 60f, 2f, Palette.Purple.rgb, Palette.Mauve.rgb, Gradient.TopLeftToBottomRight, 30f)
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

        val sw = rez.windowWidth.toFloat()
        val sh = rez.windowHeight.toFloat()

        val halfW = reveal / 2f
        val cx = sw / 2f

        val x = cx - halfW
        val width = reveal

        nvg.pushScissor(x, 0f, width, sh)
    }


    override fun onMouseClick(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        for (panel in panels) panel.mouseClicked(mx, my, button.code)
        return super.onMouseClick(button, x, y, modifiers)
    }

    override fun onMouseScroll(x: Double, y: Double, amount: Double, horizontalAmount: Double): Boolean {
        for (panel in panels) panel.mouseScrolled(mx, my, amount.toFloat(), horizontalAmount.toFloat())
        return super.onMouseScroll(x, y, amount, horizontalAmount)
    }

    override fun onMouseRelease(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        for (panel in panels) panel.mouseReleased(mx, my, button.code)
        return super.onMouseRelease(button, x, y, modifiers)
    }

    override fun onKeyPress(
        key: OmniKey,
        scanCode: Int,
        typedChar: Char,
        modifiers: KeyboardModifiers,
        event: KeyPressEvent
    ): Boolean {
        val modInt = modifiers.toMods()
        val handled = when (event) {
            KeyPressEvent.TYPED -> panels.any { it.charTyped(typedChar, modInt) }
            KeyPressEvent.PRESSED -> panels.any { it.keyPressed(key.code, modInt) }
        }

        if (handled) return true
        return super.onKeyPress(key, scanCode, typedChar, modifiers, event)
    }

    companion object {
        val caretImage = NVGRenderer.createImage( "/assets/stella/logos/dropdown.svg")
        val UI_SCALE get() = (OmniResolution.windowWidth.toFloat() / 1920f).coerceAtLeast(0.5f)
    }
}