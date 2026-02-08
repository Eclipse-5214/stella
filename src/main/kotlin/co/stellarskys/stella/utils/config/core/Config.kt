package co.stellarskys.stella.utils.config.core

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.Utils.toHex
import co.stellarskys.stella.utils.config.ui.ConfigUI
import com.google.gson.*
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.scheduling.TickSchedulers
import java.awt.Color
import java.io.File
import kotlin.reflect.KProperty

//Main config Shananagens
class Config(
    val fileName: String,
    val modID: String,
    val configPath: File? = null,
    builder: Config.() -> Unit
) {
    @PublishedApi internal val valueCache = mutableMapOf<String, Any?>()
    private val elementMap = mutableMapOf<String, ConfigElement>()
    private val categories = mutableMapOf<String, ConfigCategory>()
    private val listeners = mutableListOf<(String, Any?) -> Unit>()

    private var configUI: ConfigUI? = null
    private var loaded = false
    private var loading = false

    private val resolvedFile: File get() = configPath ?: File("config/$modID/settings.json")

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
        TickSchedulers.client.post { client.setScreen(configUI) }
    }

    // Helper functions
    fun registerListener(callback: (configName: String, value: Any?) -> Unit) { listeners += callback }

    internal fun notifyListeners(configName: String, newValue: Any?) {
        if (valueCache[configName] == newValue) return
        valueCache[configName] = newValue
        listeners.forEach { it(configName, newValue) }
        configUI?.updateUI(this)
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
            // 1. Map the elements FIRST so fromJson has something to work with
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

            // 2. Now overwrite those defaults with the file content
            if (resolvedFile.exists()) {
                val json = Gson().fromJson(resolvedFile.readText(), JsonObject::class.java)
                fromJson(json)
            }
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to load config: ${e.message}")
        } finally {
            loading = false
            loaded = true
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
        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            ensureLoaded()
            val value = valueCache[key]
                ?: run {
                    Stella.LOGGER.error("FATAL CONFIG ERROR: Missing config value for '$key'")
                    error("Missing config value for '$key'")
                }
            return if (type.isInstance(value)) {
                type.cast(value)
            } else {
                Stella.LOGGER.error("FATAL CONFIG ERROR: '$key' expected ${type.simpleName}, got ${value::class.simpleName}")
                error("Config value for '$key' is not of type ${type.simpleName}")
            }
        }

        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            val element = elementMap[key] ?: error("No config entry found for key '$key'")
            element.value = value
            notifyListeners(key, value)
        }
    }

    inline fun <reified T : Any> property(key: String): Property<T> { return Property(key, T::class.java) }
}
