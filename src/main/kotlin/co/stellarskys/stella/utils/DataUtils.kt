package co.stellarskys.stella.utils

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.JsonSerializer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import co.stellarskys.stella.utils.TimeUtils.millis
import net.fabricmc.loader.api.FabricLoader
import java.awt.Color
import java.io.File
import java.lang.reflect.Type
import java.math.BigDecimal
import java.util.concurrent.ConcurrentHashMap

class DataUtils<T: Any>(fileName: String, private val defaultObject: T, private val typeToken: TypeToken<T>? = null) {
    constructor(fileName: String, defaultObject: T) : this(fileName, defaultObject, null)
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
                override fun serialize(
                    src: Color,
                    typeOfSrc: Type,
                    context: JsonSerializationContext
                ): JsonElement {
                    val obj = JsonObject()
                    obj.addProperty("r", src.red)
                    obj.addProperty("g", src.green)
                    obj.addProperty("b", src.blue)
                    obj.addProperty("a", src.alpha)
                    return obj
                }

                override fun deserialize(
                    json: JsonElement,
                    typeOfT: Type,
                    context: JsonDeserializationContext
                ): Color {
                    val obj = json.asJsonObject
                    val r = obj.get("r").asInt
                    val g = obj.get("g").asInt
                    val b = obj.get("b").asInt
                    val a = obj.get("a").asInt
                    return Color(r, g, b, a)
                }
            })
            .create()

        private val autosaveIntervals = ConcurrentHashMap<DataUtils<*>, Long>()
        private var loopStarted = false
    }

    private val dataFile = File(
        FabricLoader.getInstance().configDir.toFile(),
        "Stella/${fileName}.json"
    )
    private var data: T = loadData()
    private var lastSavedTime = TimeUtils.now

    init {
        dataFile.parentFile.mkdirs()
        autosave(5)
        startAutosaveLoop()
        EventBus.register<GameEvent.Stop> {
            save()
        }
    }

    private fun loadData(): T {
        return try {
            if (dataFile.exists()) {
                val type = typeToken?.type ?: defaultObject::class.java
                gson.fromJson(dataFile.readText(), type) ?: defaultObject
            } else defaultObject
        } catch (e: Exception) {
            println("Error loading data from ${dataFile.absolutePath}: ${e.message}")
            defaultObject
        }
    }

    @Synchronized
    fun save() {
        try {
            dataFile.writeText(gson.toJson(data))
        } catch (e: Exception) {
            println("Error saving data to ${dataFile.absolutePath}: ${e.message}")
            e.printStackTrace()
        }
    }

    fun autosave(intervalMinutes: Long = 5) {
        autosaveIntervals[this] = intervalMinutes * 60000
    }

    fun setData(newData: T) {
        data = newData
    }

    fun getData(): T = data

    private fun startAutosaveLoop() {
        if (loopStarted) return
        loopStarted = true
        LoopUtils.loop(10000) {
            autosaveIntervals.forEach { (dataUtils, interval) ->
                if (dataUtils.lastSavedTime.since.millis < interval) return@forEach
                try {
                    val currentData = dataUtils.loadData()
                    if (currentData == dataUtils.data) return@forEach
                } catch (_: Exception) {}
                dataUtils.save()
                dataUtils.lastSavedTime = TimeUtils.now
            }
        }
    }

    operator fun invoke(): T = data

    fun update(block: T.() -> Unit) {
        block(data)
    }

    fun updateAndSave(block: T.() -> Unit) {
        update(block)
        save()
    }

    fun reset() {
        data = defaultObject
    }

    fun resetAndSave() {
        reset()
        save()
    }

    fun reload() {
        loadData()?.let { data = it }
    }

    fun copy(): T {
        return gson.fromJson(gson.toJson(data), data::class.java)
    }

    fun exists(): Boolean = dataFile.exists()

    fun delete(): Boolean {
        return try {
            dataFile.delete()
        } catch (_: Exception) {
            false
        }
    }
}