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
import com.google.gson.*
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color
import java.io.File
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer
import xyz.meowing.knit.api.scheduler.TickScheduler
import xyz.meowing.vexel.components.base.Offset
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.core.VexelWindow

//Main config Shananagens
class Config(
    configFileName: String,
    modID: String,
    file: File? = null,
    builder: Config.() -> Unit
) {
    private val categories = mutableMapOf<String, ConfigCategory>()

    private val fileName = configFileName
    private val configPath = file
    private val mod = modID
    private var loaded = false

    private var configUI: VexelScreen? = null
    private var selectedCategory: ConfigCategory? = null
    private val subcategoryLayouts = mutableListOf<SubcategoryLayout>()
    private val elementContainers = mutableMapOf<String, VexelElement<*>>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private val listeners = mutableListOf<(configName: String, value: Any?) -> Unit>()
    private val columnHeights = mutableMapOf<Int, Int>()

    private var needsVisibilityUpdate = false

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
        selectedCategory = categories.values.firstOrNull()
        EventBus.register<GameEvent.Stop> { save() }
    }

    // DSL functions
    fun category(name: String, builder: ConfigCategory.() -> Unit) {
        categories[name] = ConfigCategory(name).apply(builder)
    }

    fun markdowncategory(name: String, markdown: String){
        categories[name] = MarkdownCategory(name, markdown)
    }

    // UI builders
    private fun buildUI(initial: Boolean){
        configUI = object: VexelScreen("Config") {
            val head = Rectangle(Color(0,255,0,255).rgb)

            init {
                val bg = Rectangle(Color.BLACK.rgb,Palette.Purple.withAlpha(100).rgb, 5f, 2f)
                    .setSizing(50, Size.ParentPerc, 50, Size.ParentPerc)
                    .setPositioning(Pos.ScreenCenter, Pos.ScreenCenter)
                    .childOf(window)

                val list = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f, borderThickness = 0f)
                    .setSizing(20, Size.ParentPerc, 100, Size.ParentPerc)
                    .setPositioning(0f, Pos.ParentPixels,0f, Pos.ParentPixels)
                    .childOf(bg)

                val card = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f, borderThickness = 0f)
                    .setSizing(80, Size.ParentPerc, 100, Size.ParentPerc)
                    .setPositioning(0f,Pos.AfterSibling,0f, Pos.ParentPixels)
                    .scrollable( true)
                    .scrollbarColor(Palette.Purple.withAlpha(100).rgb)
                    .childOf(bg)

                val top = Rectangle(Color(0,0,0,0).rgb, borderRadius = 5f, borderThickness = 0f)
                    .setSizing(100, Size.ParentPerc, 40, Size.ParentPerc)
                    .setPositioning(0f, Pos.ParentPixels,0f, Pos.ParentPixels)
                    .childOf(list)

                head
                    .setSizing(12f, Size.Pixels, 12f, Size.Pixels)
                    .setPositioning(5f, Pos.ParentPercent, 5f, Pos.ParentPercent)
                    .childOf(top)

                val username = Text(KnitPlayer.player?.name?.string ?: "null", shadowEnabled = false)
                    .setPositioning(17, Pos.ParentPixels, 2, Pos.ParentPixels)
                    .childOf(head)

                val tag = Text("Stella User", Color.gray.rgb, shadowEnabled = false)
                    .setPositioning(17, Pos.ParentPixels, 12, Pos.ParentPixels)
                    .childOf(head)


                // === Category Button Panel ===

                val categoryLabels = mutableMapOf<ConfigCategory, VexelElement<xyz.meowing.vexel.elements.Button>>()


                categories.entries.forEachIndexed { _, category ->
                    // Actual button surface
                    val button = xyz.meowing.vexel.elements.Button(
                        category.key,
                        if (selectedCategory == category.value) Palette.Purple.rgb else Color.WHITE.rgb,
                        backgroundColor = if (selectedCategory == category.value) Palette.Purple.withAlpha(50).rgb else Color(0,0, 0,0).rgb,
                        borderRadius = 5f,
                        borderThickness = 0f,
                        fontSize = 16f
                    )
                        .setSizing(80f, Size.ParentPerc, 8f, Size.ParentPerc)
                        .setPositioning(0f, Pos.ParentCenter,20f, Pos.AfterSibling)
                        .childOf(list)

                    categoryLabels[category.value] = button

                    // Click handler to change category view
                    button.onMouseClick { _, _, _ ->
                        if (selectedCategory != category) {
                            selectedCategory = category.value

                            // Update label highlight colors
                            categoryLabels.forEach { (cat, btn) ->
                                btn as xyz.meowing.vexel.elements.Button
                                btn.textColor(if (cat == selectedCategory) Palette.Purple.rgb else Color.WHITE.rgb)
                                btn.backgroundColor( if (cat == selectedCategory) Palette.Purple.withAlpha(50).rgb else Color(0,0, 0,0).rgb)
                            }
                            // Destroy left over window ui
                            FloatingUIManager.clearAll()

                            // Swap out current category panel
                            card.children.clear()
                            buildCategory(card, window, category.value)
                        }
                        true
                    }
                }

                if(initial) {
                    buildCategory(card, window, selectedCategory!!)
                }
            }

            override fun isPauseScreen(): Boolean = false

            override fun onRenderGui() {
                val player = KnitPlayer.player ?: return
                val uuid = player.gameProfile.id
                val size = 24
                val x = head.scaled.left.toInt()
                val y = head.scaled.top.toInt()

                CustomGuiRenderer.render {
                    Render2D.drawPlayerHead(it, x, y, size, uuid)
                }
            }
        }
    }

    private fun buildCategory(root: VexelElement<*>, window: VexelWindow, category: ConfigCategory) {
        val Column1 = Rectangle(Color(0,0,255,255).rgb, borderRadius = 5f)
            .setSizing(50, Size.ParentPerc, 0, Size.Auto)
            .setPositioning(0f,Pos.ParentPixels,0f, Pos.ParentPixels)
            .childOf(root)

        val Column2 = Rectangle(Color(255,0,0,255).rgb, borderRadius = 5f)
            .setSizing(50, Size.ParentPerc, 0, Size.Auto)
            .setPositioning(0f,Pos.AfterSibling,0f, Pos.ParentPixels)
            .childOf(root)

        columnHeights.clear()
        subcategoryLayouts.clear()
        elementRefs.clear()
        elementContainers.clear()

        category.subcategories.entries.forEachIndexed { index, (name, subcategory) ->
            if (index % 2 == 0) {
                buildSubcategory(Column2, window, subcategory, name)
            } else {
                buildSubcategory(Column1, window, subcategory, name)
            }

        }

        val maxColumnHeight = columnHeights.values.maxOrNull() ?: 0

        /*
        val spacer = UIBlock()
            .constrain {
                width = 1.pixels()
                height = 20.pixels() // or more if you want extra breathing room
                x = CenterConstraint()
                y = maxColumnHeight.pixels()
            }
            .setChildOf(categoryContainer)
            .setColor(Color(0, 0, 0, 0)) // fully transparent
         */
    }

    private fun buildSubcategory(root: VexelElement<*>, window: VexelWindow, subcategory: ConfigSubcategory, title: String) {
        //val previousHeight = columnHeights.getOrPut(column) { 10 }
        //val boxHeight = calcSubcategoryHeight(subcategory) + 20 // extra space for title

        val box = Rectangle(Palette.Purple.withAlpha(20).rgb, Palette.Purple.withAlpha(100).rgb, 5f, 1f)
            .setSizing(90, Size.ParentPerc, 0f, Size.Auto)
            .setPositioning(0, Pos.ParentCenter, 10f, Pos.AfterSibling)
            .childOf(root)

        //columnHeights[column] = previousHeight + boxHeight + 10

        val titlebox = Rectangle(Palette.Purple.withAlpha(100).rgb)
            .setSizing(100, Size.ParentPerc, 40, Size.Pixels)
            .setPositioning(0, Pos.ParentCenter, 0, Pos.ParentPixels)
            .childOf(box)

        val titleText = Text(title, shadowEnabled = false)
            .setPositioning(5, Pos.ParentPixels, 0, Pos.ParentCenter)
            .childOf(titlebox)

        var eheight = 20

        subcategory.elements.entries.forEachIndexed { index, (key, element) ->
            val hmod = when (element) {
                is Button -> 20
                is ColorPicker -> 20
                is Dropdown -> 20
                is Keybind -> 20
                is Slider -> 20
                is StepSlider -> 20
                is TextInput -> 20
                is Toggle -> 20
                is TextParagraph -> 40 // taller for multiline text
                else -> 20 // fallback
            }

            val component = when (element) {
                //is Button -> ButtonUIBuilder().build(box, element, window)
                //is ColorPicker -> ColorPickerUIBuilder().build(box, element, window)
                //is Dropdown -> DropdownUIBuilder().build(box, element, window)
                //is Keybind -> KeybindUIBuilder().build(box, element, window)
                //is Slider -> SliderUIBuilder().build(box, element, window)
                //is StepSlider -> StepSliderUIBuilder().build(box, element, window)
                //is TextInput -> TextInputUIBuilder().build(box, element, window)
                //is TextParagraph -> TextParagraphUIBuilder().build(box, element)
                //is Toggle -> ToggleUIBuilder().build(box, element, this, window)
                else -> null
            }

            /* remove this */ eheight += hmod

            if (component == null) return@forEachIndexed

            /*
            component.constrain {
                x = CenterConstraint()
                y = eheight.pixels()
            }

             */

            elementContainers[element.configName] = component
            elementRefs[element.configName] = element

            needsVisibilityUpdate = true
            scheduleVisibilityUpdate()

            //eheight += hmod
        }

        //subcategoryLayouts += SubcategoryLayout(title, column, box, subcategory)
    }


    // UI functions
    fun open() {
        buildIfNeeded()
        TickScheduler.Client.post {
            KnitClient.client.setScreen(configUI)
        }
    }

    private fun buildIfNeeded(){
        if (configUI == null) {
            ensureLoaded()
            buildUI(true)
        }
    }

    private fun scheduleVisibilityUpdate() {
        if (!needsVisibilityUpdate) return

        elementContainers.keys.forEach { key ->
            updateElementVisibility(key)
        }

        selectedCategory?.subcategories?.forEach { (title, subcategory) ->
            recalculateElementPositions(subcategory)
        }

        restackSubcategories()

        needsVisibilityUpdate = false
    }

    private fun updateElementVisibility(configKey: String) {
        val container = elementContainers[configKey] ?: return
        val element = elementRefs[configKey] ?: return
        val visible = element.isVisible(this)

        if (visible) container.show() else container.hide()
    }

    private fun recalculateElementPositions(subcategory: ConfigSubcategory) {
        var currentY = 20 // Start below title

        subcategory.elements.forEach { (key, element) ->
            val container = elementContainers[key] ?: return@forEach
            val visible = element.isVisible(this)

            if (visible) {
                container.yConstraint = currentY.toFloat()
                container.cache.positionCacheValid = false
                currentY += getElementHeight(element)
            }
        }

        val layout = subcategoryLayouts.find { it.subcategory == subcategory } ?: return
        layout.box.height = currentY + 0f
        layout.box.cache.sizeCacheValid = false
    }

    private fun restackSubcategories() {
        val columnHeights = mutableMapOf<Int, Int>()

        subcategoryLayouts.forEach { layout ->
            val column = layout.column
            val box = layout.box
            val subcategory = layout.subcategory

            val currentHeight = columnHeights.getOrPut(column) { 10 }
            val newHeight = calcSubcategoryHeight(subcategory) + 20

            box.yConstraint = currentHeight.toFloat()
            box.cache.positionCacheValid = false
            box.height = newHeight.toFloat()
            box.cache.sizeCacheValid = false

            columnHeights[column] = currentHeight + newHeight + 10
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
        needsVisibilityUpdate = true
        scheduleVisibilityUpdate()
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

    private fun getElementHeight(element: ConfigElement): Int {
        if (!element.isVisible(this)) return 0
        return when (element) {
            is Button -> 20
            is ColorPicker -> 20
            is Dropdown -> 20
            is Keybind -> 20
            is Slider -> 20
            is StepSlider -> 20
            is TextInput -> 20
            is Toggle -> 20
            is TextParagraph -> 40 // taller for multiline text
            else -> 20 // fallback
        }
    }

    private fun calcSubcategoryHeight(subcategory: ConfigSubcategory): Int {
        return subcategory.elements.values.sumOf { element ->
            getElementHeight(element)
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
}
