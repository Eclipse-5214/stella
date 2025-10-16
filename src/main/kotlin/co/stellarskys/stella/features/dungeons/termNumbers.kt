package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.mapConfig
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.render.Render3D
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.resource.Resource
import net.minecraft.resource.ResourceManager
import net.minecraft.util.Identifier
import net.minecraft.util.math.BlockPos
import java.awt.Color
import java.io.IOException
import java.io.InputStreamReader
import kotlin.math.roundToInt

@Stella.Module
object termNumbers : Feature("termNumbers") {
    val termLabelMap: Map<String, Pair<String, Color>> = mapOf(
        "T" to ("§7( §2Tank §7)" to mapConfig.tankColor),
        "M" to ("§7( §bMage §7)" to mapConfig.mageColor),
        "B" to ("§7( §cBers §7)" to mapConfig.berzColor),
        "A" to ("§7( §6Arch §7)" to mapConfig.archerColor),
        "S" to ("§7( §6S§bt§ca§2c§dk §7)" to Color.white)
    )

    val termNumberMap: Map<Int, String> = mapOf(
        1 to "T",
        2 to "M",
        3 to "B",
        4 to "A",
        5 to "5"
    )

    override fun initialize() {
        register<RenderEvent.World> { event ->
            if (!Dungeon.inBoss() || Dungeon.floorNumber != 7) return@register

            val player: ClientPlayerEntity = MinecraftClient.getInstance().player ?: return@register
            val playerPos = Triple(
                (player.x + 0.25).roundToInt() - 1,
                player.y.roundToInt(),
                (player.z + 0.25).roundToInt() - 1
            )

            val termNumber      = config["termNumber"]      as Int
            val highlightTerms  = config["highlightTerms"]  as Boolean
            val termColor       = config["termColor"]       as RGBA
            val showTermClass   = config["showTermClass"]   as Boolean
            val classColor      = config["classColor"]      as Boolean
            val hideNumber      = config["hideNumber"]      as Boolean
            val m7Roles         = config["m7Roles"]         as Boolean
            val termClass       = config["termClass"]       as Int

            var t = termNumber + 1
            if (m7Roles) t = termClass + 1

            for ((_, phaseData) in TermRegistry.getAll()) {
                for (term in phaseData) {
                    var text = " "
                    var color = termColor.toColor()

                    // filtering
                    if (((m7Roles && term.m7ClassCode != "S") || (!m7Roles && term.classCode != "S")) && t != term.number && t != 5) continue

                    // text & color
                    if (!hideNumber || !showTermClass) text += "\n§l§8[ §f${term.number} §8]"
                    if (showTermClass) {
                        text += if (!m7Roles) {
                            "\n" + termLabelMap[term.classCode]!!.first
                        } else {
                            "\n" + termLabelMap[term.m7ClassCode]!!.first
                        }
                    }
                    if (classColor) {
                        color = if (!m7Roles) {
                            termLabelMap[term.classCode]!!.second
                        } else {
                            termLabelMap[term.m7ClassCode]!!.second
                        }
                    }

                    val pdistance = Utils.calcDistance(playerPos, Triple(term.x, term.y, term.z))

                    if (pdistance < 1600) {
                        // Draw the floating label
                        if (pdistance > 13) {
                            Render3D.renderString(
                                text,
                                term.x + 0.5, term.y + 1.95, term.z + 0.5,
                                bgBox = false, increase = true, phase = true
                            )
                        } else {
                            Render3D.renderString(
                                text,
                                term.x + 0.5, term.y + 1.95, term.z + 0.5,
                                bgBox = false, increase = false, phase = true
                            )
                        }

                        if (highlightTerms) {
                            /*
                            Render3D.renderBlock(BlockPos(term.x, term.y, term.z), partialTicks, true, color, 1f, false)

                             */
                        }
                    }
                }
            }
        }
    }
}

object TermRegistry {
    private val gson = Gson()
    private val terms = mutableMapOf<String, List<TermEntry>>() // p1, p2, etc.

    init {
        load(MinecraftClient.getInstance().resourceManager)
    }

    fun load(resourceManager: ResourceManager) {
        val id = Identifier.of(Stella.NAMESPACE, "dungeons/terms.json")
        val resource: Resource? = try {
            resourceManager.getResource(id).orElse(null)
        } catch (e: IOException) {
            println("[Terms] Error: could not find $id")
            null
        }

        if (resource == null) return

        resource.inputStream.use { stream ->
            InputStreamReader(stream).use { reader ->
                // Parse as raw map first
                val rawType = object : TypeToken<Map<String, List<List<Any>>>>() {}.type
                val rawParsed: Map<String, List<List<Any>>> = gson.fromJson(reader, rawType)

                val parsed = rawParsed.mapValues { (_, list) ->
                    list.map { arr ->
                        TermEntry(
                            x = (arr[0] as Number).toInt(),
                            y = (arr[1] as Number).toInt(),
                            z = (arr[2] as Number).toInt(),
                            number = (arr[3] as Number).toInt(),
                            classCode = arr[4] as String,
                            m7ClassCode = arr[5] as String
                        )
                    }
                }

                terms.clear()
                terms.putAll(parsed)
            }
        }
    }

    fun getPhase(name: String): List<TermEntry> = terms[name].orEmpty()
    fun getAll(): Map<String, List<TermEntry>> = terms
}

data class TermEntry(
    val x: Int,
    val y: Int,
    val z: Int,
    val number: Int,
    val classCode: String,
    val m7ClassCode: String
)
