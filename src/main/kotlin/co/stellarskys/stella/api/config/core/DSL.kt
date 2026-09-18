package co.stellarskys.stella.api.config.core

import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.KeyEvent
import java.awt.Color
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

//? if >= 26.2 {
/*import co.stellarskys.stella.api.zenith.screen
*///? }

open class ConfigCategory(val name: String, val config: Config) {
    val subcategories = mutableMapOf<String, ConfigSubcategory>()

    fun subcategory(name: String, configName: String = "", description: String = "" , builder: ConfigSubcategory.() -> Unit = {}) {
        subcategories[name] = ConfigSubcategory(
            name,
            config,
            configName,
            description
        ).apply(builder)
    }
}

open class ConfigSubcategory(val subName: String, conf: Config, confName: String, desc: String): ConfigElement() {
    val elements = mutableMapOf<String, ConfigElement>()

    init {
        config = conf
        name = subName
        description = desc
        configName = confName
        value = false
    }

    fun button(builder: Button.() -> Unit) {
        val button = Button().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[button.configName] = button
    }

    fun colorpicker(builder: ColorPicker.() -> Unit) {
        val color = ColorPicker().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[color.configName] = color
    }

    fun colorpicker(id: String, name: String, desc: String = "", def: Color = Color.WHITE, show: ((Config) -> Boolean)? = null) =
        add(ColorPicker().apply { configName = id; this.name = name; description = desc; default = def; showIf = show }, def)


    fun dropdown(builder: Dropdown.() -> Unit) {
        val dropdown = Dropdown().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[dropdown.configName] = dropdown
    }

    fun dropdown(id: String, name: String, desc: String = "", options: List<String>, def: Int = 0, show: ((Config) -> Boolean)? = null) =
        add(Dropdown().apply { configName = id; this.name = name; description = desc; this.options = options; default = def; showIf = show }, def)

    fun keybind(builder: Keybind.() -> Unit) {
        val keybind = Keybind().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[keybind.configName] = keybind
    }

    fun keybind(id: String, name: String, desc: String = "", def: Int = 0, show: ((Config) -> Boolean)? = null): ReadWriteProperty<Any?, Keybind.Handler> {
        val kb = Keybind().apply { configName = id; this.name = name; description = desc; default = def; showIf = show; config = this@ConfigSubcategory.config }
        elements[id] = kb
        config?.registerInternalElement(id, kb)
        return object : ReadWriteProperty<Any?, Keybind.Handler> {
            override fun getValue(thisRef: Any?, property: KProperty<*>) = kb.value as Keybind.Handler
            override fun setValue(thisRef: Any?, property: KProperty<*>, value: Keybind.Handler) { kb.value = value.keyCode() }
        }
    }

    fun slider(builder: Slider.() -> Unit) {
        val slider = Slider().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[slider.configName] = slider
    }

    fun slider(id: String, name: String, desc: String = "", min: Float = 0f, max: Float = 1f, def: Float = 0.5f, show: ((Config) -> Boolean)? = null) =
        add(Slider().apply { configName = id; this.name = name; description = desc; this.min = min; this.max = max; default = def; showIf = show }, def)


    fun stepslider(builder: StepSlider.() -> Unit) {
        val step = StepSlider().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[step.configName] = step
    }

    fun stepslider(id: String, name: String, desc: String = "", min: Int = 0, max: Int = 10, step: Int = 1, def: Int = 0, show: ((Config) -> Boolean)? = null) =
        add(StepSlider().apply { configName = id; this.name = name; description = desc; this.min = min; this.max = max; this.step = step; default = def; showIf = show }, def)

    fun textinput(builder: TextInput.() -> Unit) {
        val input = TextInput().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[input.configName] = input
    }

    fun textinput(id: String, name: String, desc: String = "", def: String = "", show: ((Config) -> Boolean)? = null, onChange: ((String) -> Unit)? = null) =
        add(TextInput().apply { configName = id; this.name = name; description = desc; placeholder = def; showIf = show; onValueChanged = onChange }, def)

    fun textparagraph(builder: TextParagraph.() -> Unit) {
        val para = TextParagraph().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[para.configName] = para
    }

    fun toggle(builder: Toggle.() -> Unit) {
        val toggle = Toggle().apply{ this.config = this@ConfigSubcategory.config;  builder() }
        elements[toggle.configName] = toggle
    }

    fun toggle(id: String, name: String, desc: String = "", def: Boolean = false, show: ((Config) -> Boolean)? = null) =
        add(Toggle().apply { configName = id; this.name = name; description = desc; default = def; showIf = show }, def)

    fun Button.onclick(cb: () -> Unit) {
        this.onClick = cb
    }

    fun TextInput.onvaluechange(cb: (String) -> Unit) {
        this.onValueChanged = cb
    }

    private fun <T : Any> add(el: ConfigElement, def: T): ReadWriteProperty<Any?, T> {
        el.config = this.config
        elements[el.configName] = el
        config?.registerInternalElement(el.configName, el)

        return object : ReadWriteProperty<Any?, T> {
            @Suppress("UNCHECKED_CAST")
            override fun getValue(thisRef: Any?, property: KProperty<*>): T {
                return config?.get(el.configName) as? T ?: def
            }

            override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
                el.value = value
            }
        }
    }
}

open class ConfigElement {
    var configName: String = ""
    var name: String = ""
    var description: String = ""

    private var _value: Any? = null
    open var value: Any?
        get() = _value
        set(v) {
            config?.notifyListeners(configName, _value, v)
            _value = v
        }

    var config: Config? = null
    var showIf: ((Config) -> Boolean)? = null

    fun shouldShow(predicate: (Config) -> Boolean) {
        showIf = predicate
    }

    internal fun isVisible(config: Config): Boolean {
        return showIf?.invoke(config) ?: true
    }
}

class Button : ConfigElement() {
    var placeholder: String = "Click"
    var onClick: (() -> Unit)? = null
}

class ColorPicker : ConfigElement() {
    var default: Color = Color(255, 255, 255, 255)
        set(value) {
            field = value
            this.value = value
        }

    init {
        value = default
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
            this.handler.setCode(value)
        }

    private var handler = Handler(default)

    override var value: Any?
        get() = handler
        set(value) {
            when (value) {
                is Int -> handler.setCode(value)
                else -> Stella.LOGGER.warn("Invalid value for Keybind: $value")
            }
        }

    class Handler(initCode: Int) {
        private val pressListeners = mutableListOf<() -> Unit>()
        private val releaseListeners = mutableListOf<() -> Unit>()
        private var keyCode = initCode

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

        fun onPress(block: () -> Unit) {
            pressListeners += block
        }

        fun onRelease(block: () -> Unit) {
            releaseListeners += block
        }

        fun keyCode() = keyCode
        fun setCode(newCode: Int) { keyCode = newCode}
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