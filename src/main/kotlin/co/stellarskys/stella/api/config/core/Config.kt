package co.stellarskys.stella.api.config.core

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.Utils.toHex
import co.stellarskys.stella.api.config.ui.ConfigUI
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.zenith.client
import com.google.gson.*
import java.awt.Color
import java.io.File
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.reflect.KProperty

/**
 * Main configuration system for Stella.
 * Handles the DSL building, serialization to JSON, and property delegation.
 * * @param modID Unique identifier for the mod, used for the folder name.
 * @param configPath Optional custom file path for the settings JSON.
 */
class Config(
    val modID: String,
    val configPath: File? = null,
    builder: Config.() -> Unit
) {
    @PublishedApi internal val valueCache = mutableMapOf<String, Any?>()
    private val elementMap = mutableMapOf<String, ConfigElement>()
    private val categories = mutableMapOf<String, ConfigCategory>()
    private val listeners = CopyOnWriteArrayList<(String, Any?) -> Unit>()
    private var configUI: ConfigUI? = null
    private var loaded = false
    private var loading = false

    private val resolvedFile: File get() = configPath ?: File("config/$modID/settings.json")
    val path get() = "config/$modID"

    init {
        builder()
        EventBus.on<GameEvent.Stop> { save() }
    }

    // DSL functions
    fun category(name: String, builder: ConfigCategory.() -> Unit) {
        categories[name] = ConfigCategory(name, this).apply(builder)
    }

    // UI functions
    fun open() {
        if(configUI == null) configUI = ConfigUI(categories, this)
        Chronos.Tick post { client.setScreen(configUI) }
    }

    // Helper functions
    fun registerListener(callback: (configName: String, value: Any?) -> Unit) { listeners += callback }

    internal fun notifyListeners(configName: String, newValue: Any?) {
        if (valueCache[configName] == newValue) return
        valueCache[configName] = newValue
        listeners.forEach { it(configName, newValue) }
        configUI?.updateUI(this)
    }

    internal fun registerInternalElement(id: String, element: ConfigElement) {
        elementMap[id] = element
        if (!valueCache.containsKey(id)) {
            valueCache[id] = element.value
        }
    }

    /**
     * Finds or creates a subcategory.
     * If [category] is null, it searches existing ones by Name or ConfigID.
     * If provided and not found, it creates a new box in that category.
     */
    fun subcategory(name: String, category: String? = null, configName: String = "", desc: String = ""): ConfigSubcategory {
        for (cat in categories.values) {
            cat.subcategories.values.find { it.configName == name }?.let { return it }
            cat.subcategories[name]?.let { return it }
        }

        if (category == null) { error("Could not find subcategory '$name' and no category was provided to create it!") }
        val cat = categories.getOrPut(category) { ConfigCategory(category, this) }
        val sub = ConfigSubcategory(name, this, configName, desc)
        cat.subcategories[name] = sub
        if (configName.isNotBlank()) registerInternalElement(configName, sub)

        return sub
    }

    private fun toJson() = JsonObject().apply {
        categories.values.forEach { category ->
            val subcategoryJson = JsonObject()

            category.subcategories.values.forEach { subcategory ->
                val elementJson = JsonObject()
                val id = subcategory.configName
                val value = subcategory.value

                if (id.isNotBlank() && value != null) {
                    (value as? Boolean)?.let {
                        elementJson.add(id, JsonPrimitive(value))
                    }
                }

                subcategory.elements.values.forEach { element ->
                    val id = element.configName
                    val value = element.value

                    if (id.isNotBlank() && value != null) {
                        val jsonValue = when (value) {
                            is Boolean -> JsonPrimitive(value)
                            is Int -> JsonPrimitive(value)
                            is Float -> JsonPrimitive(value)
                            is Double -> JsonPrimitive(value)
                            is String -> JsonPrimitive(value)
                            is Color -> JsonPrimitive(value.toHex())
                            is Keybind.Handler -> JsonPrimitive(value.keyCode())
                            else -> {
                                Stella.LOGGER.error("Unsupported type for $id: ${value::class.simpleName}")
                                return@forEach
                            }
                        }

                        elementJson.add(id, jsonValue)
                    }
                }

                if (elementJson.entrySet().isNotEmpty()) subcategoryJson.add(subcategory.subName, elementJson)
            }

            if (subcategoryJson.entrySet().isNotEmpty()) add(category.name, subcategoryJson)
        }
    }

    private fun fromJson(json: JsonObject) {
        elementMap.forEach { (id, element) ->
            val jsonValue = findInJson(json, id) ?: return@forEach

            val newValue: Any? = when (val current = element.value) {
                is Boolean -> jsonValue.asBoolean
                is Int -> jsonValue.asInt
                is Float -> jsonValue.asFloat
                is Double -> jsonValue.asDouble
                is String -> jsonValue.asString
                is Color -> Utils.colorFromHex(jsonValue.asString)
                is Keybind.Handler -> jsonValue.asInt
                else -> null
            }

            if (newValue != null) {
                element.value = newValue
                valueCache[id] = element.value
            }
        }
    }

    private fun findInJson(root: JsonObject, key: String): JsonElement? {
        root.entrySet().forEach { (_, cat) ->
            if (cat is JsonObject) {
                cat.entrySet().forEach { (_, sub) ->
                    if (sub is JsonObject && sub.has(key)) return sub.get(key)
                }
            }
        }
        return null
    }

    fun save() {
        try {
            resolvedFile.parentFile?.mkdirs()
            resolvedFile.writeText(GsonBuilder().setPrettyPrinting().create().toJson(toJson()))
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to save config for '$modID': ${e.message}")
            e.printStackTrace()
        }
    }

    fun load() {
        if (loading) return
        loading = true
        try {
            // Map the elements FIRST so fromJson has something to work with
            categories.values.forEach { cat ->
                cat.subcategories.values.forEach { sub ->
                    if (sub.configName.isNotBlank()) {
                        elementMap[sub.configName] = sub
                        valueCache[sub.configName] = sub.value
                    }
                    sub.elements.values.forEach { el ->
                        if (el.configName.isNotBlank()) {
                            elementMap[el.configName] = el
                            valueCache[el.configName] = el.value
                        }
                    }
                }
            }

            // Now overwrite those defaults with the file content
            if (resolvedFile.exists()) {
                try {
                    val json = Gson().fromJson(resolvedFile.readText(), JsonObject::class.java)
                    fromJson(json)
                } catch (e: Exception) {
                    Stella.LOGGER.error("Config for '$modID' is corrupted, falling back to defaults: ${e.message}")
                    backupCorruptedFile()
                }
            }
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to load config: ${e.message}")
        } finally {
            loading = false
            loaded = true
        }
    }

    /**
     * Copies the corrupted settings file aside before defaults silently overwrite it on the next [save],
     * so the user doesn't lose their old config without a trace.
     */
    private fun backupCorruptedFile() {
        try {
            val backup = File(resolvedFile.parentFile, "${resolvedFile.name}.corrupted")
            resolvedFile.copyTo(backup, overwrite = true)
            Stella.LOGGER.error("Backed up corrupted config for '$modID' to '${backup.absolutePath}'")
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to back up corrupted config for '$modID': ${e.message}")
        }
    }

    fun ensureLoaded() {
        if (!loaded && !loading) {
            load()
            loaded = true
        }
    }

    // get functions
    operator fun get(key: String): Any {
        ensureLoaded()
        return valueCache[key]
            ?: error("No config entry found for key '$key'")
    }

    inline operator fun <reified T> Config.get(key: String): T {
        ensureLoaded()
        val value = valueCache[key] ?: error("No config entry found for key '$key'")
        return value as? T ?: error("Config value for '$key' is not of expected type ${T::class.simpleName}")
    }

    inner class Property<T : Any>(
        private val key: String,
        private val type: Class<T>
    ) {
        @Suppress("UNCHECKED_CAST")
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            ensureLoaded()
            val value = valueCache[key] ?: error("FATAL CONFIG ERROR: Missing config value for '$key'")

            if (type.isInstance(value)) return type.cast(value)
            if (type.isEnum && value is Number) {
                val constants = type.enumConstants
                val index = value.toInt()
                if (index in constants.indices) return constants[index] as T
            }

            error("FATAL CONFIG ERROR: '$key' expected ${type.simpleName}, got ${value::class.simpleName}")
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val element = elementMap[key] ?: error("No config entry found for key '$key'")
            element.value = if (value is Enum<*>) value.ordinal else value
            notifyListeners(key, value)
        }
    }

    inline fun <reified T : Any> property(key: String): Property<T> { return Property(key, T::class.java) }
}
