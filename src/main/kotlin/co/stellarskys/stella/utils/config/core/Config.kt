package co.stellarskys.stella.utils.config.core

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.elements.*
import co.stellarskys.stella.utils.render.CustomGuiRenderer
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.vexel.Vexel
import com.google.gson.*
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.scheduling.TickSchedulers
import java.awt.Color
import java.io.File
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelScreen
import co.stellarskys.vexel.core.VexelWindow
import kotlin.reflect.KProperty

//Main config Shananagens
class Config(
    configFileName: String,
    modID: String,
    file: File? = null,
    builder: Config.() -> Unit
) {
    val categories = mutableMapOf<String, ConfigCategory>()

    private val fileName = configFileName
    private val configPath = file
    private val mod = modID
    private var loaded = false

    private var configUI: ConfigUI? = null
    private val listeners = mutableListOf<(configName: String, value: Any?) -> Unit>()

    private val resolvedFile: File
        get() = configPath ?: File("config/$mod/settings.json")

    data class SubcategoryLayout(
        val title: String,
        val column: Int,
        val box: VexelElement<*>,
        val subcategory: ConfigSubcategory
    )

    init {
        this.builder()
        EventBus.on<GameEvent.Stop> { save() }
    }

    // DSL functions
    fun category(name: String, builder: ConfigCategory.() -> Unit) {
        categories[name] = ConfigCategory(name).apply(builder)
    }

    fun markdowncategory(name: String, markdown: String){
        categories[name] = MarkdownCategory(name, markdown)
    }

    // UI builders
    private class ConfigUI(categories: Map<String, ConfigCategory>, config: Config): VexelScreen("Config") {
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
                .setSizing(100f, Size.Percent, 40f, Size.Percent)
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

        override fun onRenderGui() {
            Vexel.renderer.endFrame()

            val player = player ?: return
            val uuid = player.gameProfile.id
            val size = (48 / OmniResolution.scaleFactor).toInt()
            val x = (head.raw.left / OmniResolution.scaleFactor).toInt()
            val y = (head.raw.top/ OmniResolution.scaleFactor).toInt()

            CustomGuiRenderer.render {
                Render2D.drawPlayerHead(it, x, y, size, uuid)
            }

            Vexel.renderer.beginFrame(0f,0f)
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
            //val previousHeight = columnHeights.getOrPut(column) { 10 }
            //val boxHeight = calcSubcategoryHeight(subcategory) + 20 // extra space for title

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
                    //is Keybind -> KeybindUIBuilder().build(box, element, window)
                    is Slider -> SliderUIBuilder().build(box, element, window)
                    is StepSlider -> StepSliderUIBuilder().build(box, element, window)
                    //is TextInput -> TextInputUIBuilder().build(box, element, window)
                    is TextParagraph -> TextParagraphUIBuilder().build(box, element)
                    is Toggle -> ToggleUIBuilder().build(box, element, config, window)
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

    // UI functions
    fun open() {
        configUI = ConfigUI(categories, this)
        TickSchedulers.client.post {
            client.setScreen(configUI)
        }
    }

    // Helper functions
    fun flattenValues(): Map<String, Any?> {
        return categories
            .flatMap { (_, category) ->
                category.subcategories
                    .flatMap { (_, subcategory) ->
                        subcategory.elements.values
                    }
            }
            .associate { it.configName to it.value }
    }


    fun registerListener(callback: (configName: String, value: Any?) -> Unit) {
        listeners += callback
    }

    internal fun notifyListeners(configName: String, newValue: Any?) {
        listeners.forEach { it(configName, newValue) }
        updateConfig()
    }

    private fun updateConfig() {
        configUI?.updateUI(this)
    }

    private fun toJson(): JsonObject {
        val root = JsonObject()

        categories.forEach { (_, category) ->
            val subcategoryJson = JsonObject()

            category.subcategories.forEach { (_, subcategory) ->
                val elementJson = JsonObject()

                subcategory.elements.forEach { (_, element) ->
                    val id = element.configName
                    val value = element.value

                    if (id.isNotBlank() && value != null) {
                        val jsonValue = when (value) {
                            is Boolean -> JsonPrimitive(value)
                            is Int -> JsonPrimitive(value)
                            is Float -> JsonPrimitive(value)
                            is Double -> JsonPrimitive(value)
                            is String -> JsonPrimitive(value)
                            is RGBA -> JsonPrimitive(value.toHex())
                            else -> {
                                Stella.LOGGER.error("Unsupported type for $id: ${value::class.simpleName}")
                                return@forEach
                            }
                        }

                        elementJson.add(id, jsonValue)
                    }
                }

                if (elementJson.entrySet().isNotEmpty()) {
                    subcategoryJson.add(subcategory.subName, elementJson)
                }
            }

            if (subcategoryJson.entrySet().isNotEmpty()) {
                root.add(category.name, subcategoryJson)
            }
        }

        return root
    }

    private fun fromJson(json: JsonObject) {
        categories.forEach { (_, category) ->
            val categoryData = json.getAsJsonObject(category.name) ?: return@forEach

            category.subcategories.forEach { (_, subcategory) ->
                val subcategoryData = categoryData.getAsJsonObject(subcategory.subName) ?: return@forEach

                subcategory.elements.forEach { (_, element) ->
                    val id = element.configName
                    val jsonValue = subcategoryData.get(id) ?: return@forEach

                    val newValue = when (val current = element.value) {
                        is Boolean -> jsonValue.asBoolean
                        is Int -> jsonValue.asInt
                        is Float -> jsonValue.asFloat
                        is Double -> jsonValue.asDouble
                        is String -> jsonValue.asString
                        is RGBA -> RGBA.fromHex(jsonValue.asString)
                        else -> {
                            Stella.LOGGER.warn("Skipping unsupported load type for '$id': ${current?.let { it::class.simpleName } ?: "null"}")
                            null
                        }
                    }

                    if (newValue != null) element.value = newValue
                }
            }
        }
    }

    fun save() {
        try {
            val target = resolvedFile
            target.parentFile?.mkdirs()

            val json = toJson()
            val gson = GsonBuilder().setPrettyPrinting().create()
            val jsonString = gson.toJson(json)

            target.writeText(jsonString)
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to save config for '$mod': ${e.message}")
            e.printStackTrace()
        }
    }

    fun load() {
        try {
            val target = resolvedFile
            if (!target.exists()) return

            val jsonText = target.readText()
            val gson = Gson()
            val loadedJson = gson.fromJson(jsonText, JsonObject::class.java)

            fromJson(loadedJson)
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to load config for '$mod': ${e.message}")
            e.printStackTrace()
        }
    }

    fun ensureLoaded() {
        if (!loaded) {
            load()
            loaded = true
        }
    }

    // get functions
    operator fun get(key: String): Any {
        ensureLoaded()
        return flattenValues()[key]
            ?: error("No config entry found for key '$key'")
    }

    inline operator fun <reified T> Config.get(key: String): T {
        ensureLoaded()
        val value = flattenValues()[key]
            ?: error("No config entry found for key '$key'")

        return value as? T
            ?: error("Config value for '$key' is not of expected type ${T::class.simpleName}")
    }

    inline fun <reified T> getValue(key: String): T {
        ensureLoaded()
        val value = flattenValues()[key]
            ?: error("Missing config value for '$key'")

        return value as? T
            ?: error("Config value for '$key' is not of type ${T::class.simpleName}")
    }

    inner class Property<T : Any>(
        private val key: String,
        private val type: Class<T>
    ) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val value = flattenValues()[key]
                ?: error("Missing config value for '$key'")

            return if (type.isInstance(value)) {
                type.cast(value)
            } else {
                error("Config value for '$key' is not of type ${type.simpleName}")
            }
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val element = categories.values
                .flatMap { it.subcategories.values }
                .flatMap { it.elements.values }
                .firstOrNull { it.configName == key }
                ?: error("No config entry found for key '$key'")

            element.value = value
            notifyListeners(key, value)
        }
    }

    inline fun <reified T : Any> property(key: String): Property<T> { return Property(key, T::class.java) }
}
