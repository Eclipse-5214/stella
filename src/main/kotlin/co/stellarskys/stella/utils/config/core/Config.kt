package co.stellarskys.stella.utils.config.core

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.GameEvent
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.UCRenderPipelines
import co.stellarskys.stella.utils.config.drawTexture
import co.stellarskys.stella.utils.config.ui.elements.*
import com.google.gson.*
import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.UIComponent
import gg.essential.elementa.WindowScreen
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import gg.essential.elementa.markdown.MarkdownComponent
import gg.essential.universal.UMatrixStack
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.entity.player.PlayerModelPart
import java.awt.Color
import java.io.File

//TODO Use gradient for the Slider

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

    private var configUI: WindowScreen? = null
    private var selectedCategory: ConfigCategory? = null
    private val subcategoryLayouts = mutableListOf<SubcategoryLayout>()
    private val elementContainers = mutableMapOf<String, UIComponent>()
    private val elementRefs = mutableMapOf<String, ConfigElement>()
    private val listeners = mutableListOf<(configName: String, value: Any?) -> Unit>()
    private val columnHeights = mutableMapOf<Int, Int>()

    private var needsVisibilityUpdate = false

    private val resolvedFile: File
        get() = configPath ?: File("config/$mod/settings.json")

    data class SubcategoryLayout(
        val title: String,
        val column: Int,
        val box: UIComponent,
        val subcategory: ConfigSubcategory
    )

    init {
        this.builder()
        selectedCategory = categories.values.firstOrNull()
        EventBus.register<GameEvent.Unload> { save() }
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
        configUI = object: WindowScreen(ElementaVersion.V2) {
            val head = UIBlock(Color(0,0,0,0))

            init {
                val listBG = createBlock(5f)
                    .constrain {
                        width = 502.pixels()
                        height = 252.pixels()
                        x = CenterConstraint()
                        y = CenterConstraint()
                    }
                    .setColor(Palette.Purple.withAlpha(100))
                    .setChildOf(window)

                val list = createBlock(5f)
                    .constrain {
                        width = 500.pixels()
                        height = 250.pixels()
                        x = CenterConstraint()
                        y = CenterConstraint()
                    }
                    .setColor(Color.BLACK)
                    .setChildOf(window)

                val card = createBlock(5f)
                    .constrain {
                        width = 400.pixels()
                        height = 250.pixels()
                        x = CenterConstraint() + 50.pixels() // Offset to the right of list panel
                        y = CenterConstraint()
                    }
                    .setColor(Color.black)
                    .setChildOf(window)

                val title = UIText(fileName, false)
                    .constrain {
                        x = CenterConstraint() - 237.pixels()
                        y = CenterConstraint() - 160.pixels()
                    }
                    .setTextScale(1.5f.pixels())
                    .setChildOf(window)

                head
                    .constrain {
                    x = CenterConstraint() - 235.pixels()
                    y = CenterConstraint() - 110.pixels()
                    width = PixelConstraint(12f)
                    height = PixelConstraint(12f)
                }.setChildOf(window)

                //#if MC >= 1.21.5
                val username = UIText(Stella.mc.player?.name?.string ?: "null", false)
                    //#elseif MC == 1.8.9
                    //$$ val username = UIText(Stella.mc.thePlayer?.name ?: "null", false)
                    //#endif

                    .constrain {
                        x = RelativeConstraint() + 17.pixels()
                        y = CenterConstraint() + 2.pixels()
                    }
                    .setColor(Color.white)
                    .setChildOf(head)

                val tag = UIText("Stella User", false)
                    .constrain {
                        x = RelativeConstraint() + 17.pixels()
                        y = CenterConstraint() + 12.pixels()
                    }
                    .setColor(Color.gray)
                    .setChildOf(head)


                // === Category Button Panel ===

                val categoryLabels = mutableMapOf<ConfigCategory, Pair<UIComponent, UIComponent>>()


                categories.entries.forEachIndexed { index, category ->
                    // Actual button surface
                    val button = createBlock(3f)
                        .constrain {
                            width = 80.pixels()
                            height = 20.pixels()
                            x = CenterConstraint() - 200.pixels()
                            y = CenterConstraint() + (index * 30).pixels()
                        }
                        .setColor(if (selectedCategory == category.value) Palette.Purple.withAlpha(50) else Color(0,0, 0,0))
                        .setChildOf(window)

                    // Category label text
                    val label = UIWrappedText(category.key, centered = true)
                        .constrain {
                            x = CenterConstraint()
                            y = CenterConstraint()
                            width = 76.pixels()
                        }
                        .setColor(if (selectedCategory == category.value) Palette.Purple else Color.WHITE)
                        .setChildOf(button)

                    categoryLabels[category.value] = Pair(button, label)

                    // Click handler to change category view
                    button.onMouseClick {
                        if (selectedCategory != category) {
                            selectedCategory = category.value

                            // Update label highlight colors
                            categoryLabels.forEach { (cat, button) ->
                                val btn = button.first
                                val lbl = button.second

                                lbl.animate{
                                    setColorAnimation(
                                        Animations.OUT_CUBIC,
                                        0.3f,
                                        if (cat == selectedCategory) Palette.Purple.toConstraint() else Color.WHITE.toConstraint()
                                    )
                                }

                                btn.animate {
                                    setColorAnimation(
                                        Animations.OUT_CUBIC,
                                        0.3f,
                                        if (cat == selectedCategory) Palette.Purple.withAlpha(50).toConstraint()
                                        else Color(0,0, 0,0).toConstraint()
                                    )
                                }
                            }

                            // Destroy left over window ui
                            FloatingUIManager.clearAll()

                            // Swap out current category panel
                            card.clearChildren()

                            if (category.value.isMarkdown) buildMarkown(card, category.value)
                            else buildCategory(card, window, category.value)
                        }
                    }
                }

                if(initial) {
                    if (selectedCategory!!.isMarkdown) buildMarkown(card, selectedCategory!!)
                    else buildCategory(card, window, selectedCategory!!)
                }
            }

            //#if MC >= 1.21.5
            override fun shouldPause(): Boolean = false
            //#endif

            override fun onDrawScreen(matrixStack: UMatrixStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
                super.onDrawScreen(matrixStack, mouseX, mouseY, partialTicks)

                val player = Stella.mc.player ?: return
                val entry = Stella.mc.networkHandler?.getPlayerListEntry(player.uuid)
                val skin = entry?.skinTextures?.comp_1626  ?: DefaultSkinHelper.getTexture()
                val hasHat = player.isPartVisible(PlayerModelPart.HAT)

                val x = head.getLeft().toDouble()
                val y = head.getTop().toDouble()
                val size = 24.0

                drawTexture(matrixStack, UCRenderPipelines.guiTexturePipeline, skin, x, y, size, size, 8.0, 8.0, 8.0, 8.0)

                if (hasHat) {
                    drawTexture(matrixStack, UCRenderPipelines.guiTexturePipeline, skin, x, y, size, size, 40.0, 8.0, 8.0, 8.0)
                }
            }
        }
    }

    private fun buildMarkown(root: UIComponent, category: ConfigCategory){
        val catagoryContainer = ScrollComponent()
            .constrain {
                width = 450.pixels()
                height = 325.pixels()
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(root)

        val markdown = MarkdownComponent(category.markdown)
            .constrain {
                width = RelativeConstraint(1f)
                height = RelativeConstraint(1f)
                x = CenterConstraint()
                y = PixelConstraint(2f)
            }
            .setChildOf(catagoryContainer)
    }

    private fun buildCategory(root: UIComponent, window: Window, category: ConfigCategory) {
        val categoryContainer = ScrollComponent()
            .constrain {
                width = 400.pixels()
                height = 250.pixels()
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(root)

        columnHeights.clear()
        subcategoryLayouts.clear()
        elementRefs.clear()
        elementContainers.clear()

        category.subcategories.entries.forEachIndexed { index, (name, subcategory) ->
            val column = index % 2
            val row = index / 2

            buildSubcategory(categoryContainer, window, subcategory, name, row, column)
        }

        val maxColumnHeight = columnHeights.values.maxOrNull() ?: 0

        val spacer = UIBlock()
            .constrain {
                width = 1.pixels()
                height = 20.pixels() // or more if you want extra breathing room
                x = CenterConstraint()
                y = maxColumnHeight.pixels()
            }
            .setChildOf(categoryContainer)
            .setColor(Color(0, 0, 0, 0)) // fully transparent
    }

    private fun buildSubcategory(root: UIComponent, window: Window, subcategory: ConfigSubcategory, title: String, row: Int, column: Int) {
        val previousHeight = columnHeights.getOrPut(column) { 10 }
        val boxHeight = calcSubcategoryHeight(subcategory) + 20 // extra space for title

        val box = UIBlock()
            .constrain {
                width = 180.pixels()
                height = boxHeight.pixels()
                x = CenterConstraint() - 100.pixels() + (200 * column).pixels()
                y = previousHeight.pixels()
            }
            .setChildOf(root)
            .setColor(Palette.Purple.withAlpha(20))
            .effect(OutlineEffect(Palette.Purple.withAlpha(100), 1f))

        columnHeights[column] = previousHeight + boxHeight + 10

        val titlebox = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = 0.pixels()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(box)

        val titleText = UIText(title, false)
            .constrain {
                x = 5.pixels()
                y = CenterConstraint()
            }
            .setChildOf(titlebox)
            .setColor(Color.WHITE)

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
                is Button -> ButtonUIBuilder().build(box, element, window)
                is ColorPicker -> ColorPickerUIBuilder().build(box, element, window)
                is Dropdown -> DropdownUIBuilder().build(box, element, window)
                is Keybind -> KeybindUIBuilder().build(box, element, window)
                is Slider -> SliderUIBuilder().build(box, element, window)
                is StepSlider -> StepSliderUIBuilder().build(box, element, window)
                is TextInput -> TextInputUIBuilder().build(box, element, window)
                is TextParagraph -> TextParagraphUIBuilder().build(box, element)
                is Toggle -> ToggleUIBuilder().build(box, element, this, window)
                else -> null
            }

            if (component == null) return@forEachIndexed

            component.constrain {
                x = CenterConstraint()
                y = eheight.pixels()
            }

            elementContainers[element.configName] = component
            elementRefs[element.configName] = element

            needsVisibilityUpdate = true
            scheduleVisibilityUpdate()

            eheight += hmod
        }

        subcategoryLayouts += SubcategoryLayout(title, column, box, subcategory)
    }


    // UI functions
    fun open() {
        buildIfNeeded()
        TickUtils.schedule(1){
            //#if MC > 1.21.5
            Stella.mc.setScreen(configUI)
            //#elseif MC == 1.8.9
            //$$ Stella.mc.displayGuiScreen(configUI)
            //#endif
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

        if (visible) container.unhide(true) else container.hide(true)
    }

    private fun recalculateElementPositions(subcategory: ConfigSubcategory) {
        var currentY = 20 // Start below title

        subcategory.elements.forEach { (key, element) ->
            val container = elementContainers[key] ?: return@forEach
            val visible = element.isVisible(this)

            if (visible) {
                container.setY(currentY.pixels())
                currentY += getElementHeight(element)
            }
        }

        val layout = subcategoryLayouts.find { it.subcategory == subcategory } ?: return
        layout.box.setHeight((currentY + 0).pixels()) // +0 for padding if needed
    }

    private fun restackSubcategories() {
        val columnHeights = mutableMapOf<Int, Int>()

        subcategoryLayouts.forEach { layout ->
            val column = layout.column
            val box = layout.box
            val subcategory = layout.subcategory

            val currentHeight = columnHeights.getOrPut(column) { 10 }
            val newHeight = calcSubcategoryHeight(subcategory) + 20

            box.setY(currentHeight.pixels())
            box.setHeight(newHeight.pixels())

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
        println("visibility update called")
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
                                println("Unsupported type for $id: ${value::class.simpleName}")
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
                            println("Skipping unsupported load type for '$id': ${current?.let { it::class.simpleName } ?: "null"}")
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
            println("Failed to save config for '$mod': ${e.message}")
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
            println("Failed to load config for '$mod': ${e.message}")
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
