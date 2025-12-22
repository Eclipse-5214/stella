package co.stellarskys.stella.utils.config.core

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.KeyEvent
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.vexel.Vexel
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelWindow
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
            this.value = Handler(value)
        }

    init {
        value = Handler(default)
    }

    class Handler(keyCode: Int) {
        private val pressListeners = mutableListOf<() -> Unit>()
        private val releaseListeners = mutableListOf<() -> Unit>()

        var isDown = false
            private set

            init {
                EventBus.on<KeyEvent.Press> {
                    if (it.keyCode == keyCode && !isDown) {
                        isDown = true
                        pressListeners.forEach { fn -> fn() }
                    }
                }

                EventBus.on<KeyEvent.Release> {
                    if (it.keyCode == keyCode && isDown) {
                        isDown = false
                        releaseListeners.forEach { fn -> fn() }
                    }
                }
            }

        // Register listeners
        fun onPress(block: () -> Unit) {
            pressListeners += block
        }

        fun onRelease(block: () -> Unit) {
            releaseListeners += block
        }
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
    private val floatingComponents = mutableListOf<VexelElement<*>>()

    fun register(component: VexelElement<*>) {
        floatingComponents += component
    }

    fun clearAll() {
        floatingComponents.forEach { it.destroy() }
        floatingComponents.clear()
    }
}

fun attachTooltip(window: VexelWindow, anchor: VexelElement<*>, description: String) {
    if (description == "") return

    val fs = 14f
    val width = Vexel.renderer.textWidth(description, fs, Vexel.defaultFont)

    val tooltip = Rectangle(Color.black.rgb, borderRadius = 5f, borderThickness = 0f)
        .setPositioning(Pos.ScreenCenter, Pos.ScreenCenter)
        .setSizing(width + 10f, Size.Pixels, fs + 10f, Size.Pixels)
        .setOffset(0f, 300f)
        .childOf(window)

    val tooltipText = Text(description, fontSize = fs)
        .setPositioning(Pos.ParentCenter, Pos.ParentCenter)
        .childOf(tooltip)

    tooltip.hide()

    anchor.onMouseEnter { _ -> tooltip.show() }
    anchor.onMouseExit { _ -> tooltip.hide() }

    FloatingUIManager.register(tooltip)
}

