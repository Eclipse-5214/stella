package co.stellarskys.stella.api.handlers

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import net.fabricmc.loader.api.FabricLoader
import java.awt.Color
import java.io.File
import kotlin.reflect.KProperty
import java.lang.reflect.Type
import java.math.BigDecimal

class Capsule<T: Any>(fileName: String, private val defaultObject: T, private val typeToken: TypeToken<T>? = null) {
    constructor(fileName: String, defaultObject: T) : this(fileName, defaultObject, null)

    private val dataFile = File(FabricLoader.getInstance().configDir.toFile().resolve(Stella.NAMESPACE), "${fileName}.json")
    private var data: T = loadData()

    init {
        dataFile.parentFile.mkdirs()
        EventBus.on<GameEvent.Stop> { save() }
    }

    private fun loadData(): T {
        return try {
            if (dataFile.exists()) {
                val type = typeToken?.type ?: defaultObject::class.java
                gson.fromJson(dataFile.readText(), type) ?: defaultObject
            } else defaultObject
        } catch (e: Exception) {
            Stella.LOGGER.error("Error loading data from ${dataFile.absolutePath}: ${e.message}")
            defaultObject
        }
    }

    @Synchronized
    fun save() {
        try { dataFile.writeText(gson.toJson(data))
        } catch (e: Exception) {
            Stella.LOGGER.error("Error saving data to ${dataFile.absolutePath}: ${e.message}")
        }
    }

    fun setData(newData: T) { data = newData; save() }
    fun getData(): T = data
    operator fun invoke(): T = data
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = data
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { data = value; save() }

    fun update(block: T.() -> Unit) { block(data); save() }

    fun reset() {
        val type = typeToken?.type ?: defaultObject::class.java
        data = gson.fromJson(gson.toJson(defaultObject), type)
        save()
    }

    fun reload() { data = loadData() }
    fun exists(): Boolean = dataFile.exists()

    fun copy(): T {
        val type = typeToken?.type ?: data::class.java
        return gson.fromJson(gson.toJson(data), type)
    }

    fun delete(): Boolean {
        return try { dataFile.delete() } catch (_: Exception) { false }
    }

    companion object {
        private val gson = GsonBuilder()
            .setPrettyPrinting()
            .setObjectToNumberStrategy { reader ->
                val value = reader.nextString()
                val bd = value.toBigDecimal()
                when {
                    bd.scale() <= 0 && bd <= BigDecimal.valueOf(Int.MAX_VALUE.toLong()) && bd >= BigDecimal.valueOf(Int.MIN_VALUE.toLong()) -> bd.intValueExact()
                    else -> bd.toDouble()
                }
            }
            .registerTypeAdapter(Color::class.java, object : JsonSerializer<Color>, JsonDeserializer<Color> {
                override fun serialize(src: Color, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
                    val obj = JsonObject()
                    obj.addProperty("r", src.red)
                    obj.addProperty("g", src.green)
                    obj.addProperty("b", src.blue)
                    obj.addProperty("a", src.alpha)
                    return obj
                }

                override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Color {
                    val obj = json.asJsonObject
                    return Color(obj.get("r").asInt, obj.get("g").asInt, obj.get("b").asInt, obj.get("a").asInt)
                }
            })
            .create()
    }
}
