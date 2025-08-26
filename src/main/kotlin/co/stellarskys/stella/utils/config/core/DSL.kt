package co.stellarskys.stella.utils.config.core

import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.render.Render2D.width
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import java.awt.Color

open class ConfigCategory(val name: String) {
    val subcategories = mutableMapOf<String, ConfigSubcategory>()
    var isMarkdown = false
    var markdown = ""

    fun subcategory(name: String, builder: ConfigSubcategory.() -> Unit) {
        subcategories[name] = ConfigSubcategory(name).apply(builder)
    }
}

open class ConfigSubcategory(val subName: String) {
    val elements = mutableMapOf<String, ConfigElement>()

    /**
     * Adds a [Button] element to the config category.
     *
     * @param builder Lambda used to configure the Button.
     */
    fun button(builder: Button.() -> Unit) {
        val button = Button().apply(builder)
        elements[button.configName] = button
    }

    /**
     * Adds a [ColorPicker] element to the config category.
     *
     * @param builder Lambda used to configure the ColorPicker.
     */
    fun colorpicker(builder: ColorPicker.() -> Unit) {
        val color = ColorPicker().apply(builder)
        elements[color.configName] = color
    }

    /**
     * Adds a [Dropdown] element to the config category.
     *
     * @param builder Lambda used to configure the Dropdown.
     */
    fun dropdown(builder: Dropdown.() -> Unit) {
        val dropdown = Dropdown().apply(builder)
        elements[dropdown.configName] = dropdown
    }

    /**
     * Adds a [Keybind] element to the config category.
     *
     * @param builder Lambda used to configure the Keybind.
     */
    fun keybind(builder: Keybind.() -> Unit) {
        val keybind = Keybind().apply(builder)
        elements[keybind.configName] = keybind
    }

    /**
     * Adds a [Slider] element to the config category.
     *
     * @param builder Lambda used to configure the Slider.
     */
    fun slider(builder: Slider.() -> Unit) {
        val slider = Slider().apply(builder)
        elements[slider.configName] = slider
    }

    /**
     * Adds a [StepSlider] element to the config category.
     *
     * @param builder Lambda used to configure the StepSlider.
     */
    fun stepslider(builder: StepSlider.() -> Unit) {
        val step = StepSlider().apply(builder)
        elements[step.configName] = step
    }

    /**
     * Adds a [TextInput] element to the config category.
     *
     * @param builder Lambda used to configure the TextInput.
     */
    fun textinput(builder: TextInput.() -> Unit) {
        val input = TextInput().apply(builder)
        elements[input.configName] = input
    }

    /**
     * Adds a [TextParagraph] element to the config category.
     *
     * @param builder Lambda used to configure the TextParagraph.
     */
    fun textparagraph(builder: TextParagraph.() -> Unit) {
        val para = TextParagraph().apply(builder)
        elements[para.configName] = para
    }

    /**
     * Adds a [Toggle] element (boolean switch) to the config category.
     *
     * @param builder Lambda used to configure the Toggle.
     */
    fun toggle(builder: Toggle.() -> Unit) {
        val toggle = Toggle().apply(builder)
        elements[toggle.configName] = toggle
    }

    /**
     * Sets the click behavior for a [Button].
     *
     * @param cb A lambda that will be executed when the button is clicked.
     */
    fun Button.onclick(cb: () -> Unit) {
        this.onClick = cb
    }

    /**
     * Sets the value change listener for a [TextInput].
     *
     * @param cb A lambda that will be triggered whenever the value changes.
     */
    fun TextInput.onvaluechange(cb: (String) -> Unit) {
        this.onValueChanged = cb
    }
}


/**
 * A special category type that displays static markdown content.
 *
 * Used for rendering documentation, tutorials, or decorative sections
 * within a NovaConfig UI.
 *
 * @param mdName The display name of the category.
 * @param md The markdown string to be rendered.
 */
class MarkdownCategory(val mdName: String, val md: String): ConfigCategory(md){
    init {
        isMarkdown = true
        markdown = md
    }
}

open class ConfigElement {
    /** Unique identifier for the element used in saving/loading. */
    var configName: String = ""

    /** Display name shown in the UI. */
    var name: String = ""

    /** Short description, shown as hover text or under headers. */
    var description: String = ""

    /** The current value of the element (can be Boolean, String, Int, etc). */
    var value: Any? = null

    var showIf: ((Map<String, Any?>) -> Boolean)? = null

    /**
     * Assigns a conditional visibility predicate.
     *
     * @param predicate Lambda that receives flattened config values.
     */
    fun shouldShow(predicate: (Map<String, Any?>) -> Boolean) {
        showIf = predicate
    }

    internal fun isVisible(config: Config): Boolean {
        val flat = config.flattenValues()
        return showIf?.invoke(flat) ?: true
    }
}

// Elements
class Button : ConfigElement() {
    var placeholder: String = "Click"
    var onClick: (() -> Unit)? = null
}

class ColorPicker : ConfigElement() {
    var default: RGBA = RGBA(255, 255, 255, 255)
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = default
    }

    fun rgba(r: Int, g: Int, b: Int, a: Int): RGBA {
        return RGBA(r, g, b, a)
    }
}

class Dropdown : ConfigElement() {
    var options: List<String> = listOf()
    var default: Int = 0
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = default
    }
}

class Keybind : ConfigElement() {
    var default: Int = 0
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = default
    }
}

class Slider : ConfigElement() {
    var min: Float = 0f
    var max: Float = 1f
    var default: Float = 0.5f
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = default
    }
}

class StepSlider : ConfigElement() {
    var min: Int = 0
    var max: Int = 10
    var step: Int = 1
    var default: Int = 0
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = default
    }
}

class Subcategory : ConfigElement()

class TextInput : ConfigElement() {
    var placeholder: String = ""
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = placeholder
    }

    var onValueChanged: ((String) -> Unit)? = null
}

class TextParagraph : ConfigElement()

class Toggle : ConfigElement() {
    var default: Boolean = false
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = default
    }
}

object FloatingUIManager {
    private val floatingComponents = mutableListOf<UIComponent>()
    private val colorPickers = mutableListOf<UIComponent>()
    private var activePicker: UIComponent? = null


    fun register(component: UIComponent) {
        floatingComponents += component
    }

    fun registerColorPicker(picker: UIComponent) {
        colorPickers += picker
        register(picker)
    }

    fun setActivePicker(active: UIComponent) {
        colorPickers.forEach { it.hide() }
        active.unhide()
        activePicker = active
    }

    fun getActivePicker(): UIComponent? = activePicker

    fun clearActivePicker() {
        activePicker?.hide()
        activePicker = null
    }

    fun clearAll() {
        floatingComponents.forEach { it.parent.removeChild(it) }
        floatingComponents.clear()
        colorPickers.clear()
    }
}

fun UIComponent.attachToWindow(window: Window): UIComponent {
    this.setChildOf(window)
    FloatingUIManager.register(this)
    return this
}

fun attachTooltip(window: Window, anchor: UIComponent, description: String) {
    if (description == "") return

    val tooltip = createBlock(3f)
        .constrain {
            width = (description.width() + 10).pixels()
            height = 20.pixels()
            x = CenterConstraint()
            y = CenterConstraint() + 150.pixels()
        }
        .setColor(Color.black)
        .setChildOf(window)

    val tooltipText = UIText(description)
        .constrain {
            x = CenterConstraint()
            y = CenterConstraint()
        }
        .setColor(Color.WHITE)
        .setChildOf(tooltip)

    tooltip.hide(true)

    anchor.onMouseEnter { tooltip.unhide(true) }
    anchor.onMouseLeave { tooltip.hide(true) }

    FloatingUIManager.register(tooltip)
    FloatingUIManager.register(tooltipText)
}

