package co.stellarskys.stella.features.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.mapConfig
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.utils.render.RenderContext
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import com.google.gson.Gson
import com.google.gson.JsonObject
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import java.awt.Color
import java.io.InputStreamReader
import kotlin.math.roundToInt

@Module
object termNumbers : Feature("termNumbers", island = SkyBlockIsland.THE_CATACOMBS) {
    private val presetKeys = listOf("f7", "super_low_m7", "low_m7", "mid_m7", "high_m7")
    private val roleKeys = listOf("tank", "mage", "berserk", "archer", "healer", "all")

    val termLabelMap: Map<String, Pair<String, Color>> = mapOf(
        "tank" to ("§7( §2Tank §7)" to mapConfig.tankColor),
        "mage" to ("§7( §bMage §7)" to mapConfig.mageColor),
        "berserk" to ("§7( §cBers §7)" to mapConfig.berzColor),
        "archer" to ("§7( §6Arch §7)" to mapConfig.archerColor),
        "healer" to ("§7( §dHeal §7)" to mapConfig.healerColor),
        "stack" to ("§7( §6S§bt§ca§2c§dk §7)" to Color.white)
    )

    val selectedRole by config.property<Int>("selectedRole")
    val preset by config.property<Int>("preset")

    val showTermClass by config.property<Boolean>("showTermClass")
    val hideNumber by config.property<Boolean>("hideNumber")
    val classColor by config.property<Boolean>("classColor")

    val highlightTerms by config.property<Boolean>("highlightTerms")
    val termColor by config.property<RGBA>("termColor")

    enum class TaskType(val jsonKey: String, val coordKey: String) {
        TERMINAL("term", "terms"),
        LEVER("lever", "levers"),
        DEVICE("device", "devices");
    }

    data class TaskAssignment(
        val id: Int,
        val type: TaskType,
        val roles: MutableList<String> = mutableListOf()
    )

    override fun initialize() {
        on<RenderEvent.World.Last> { event ->
            if (!Dungeon.inBoss || Dungeon.floorNumber != 7) return@on

            val currentPresetKey = presetKeys.getOrElse(preset) { "f7" }
            val currentRoleKey = roleKeys.getOrElse(selectedRole) { "all" }

            val player = player ?: return@on
            val playerPos = Triple(
                (player.x + 0.25).roundToInt() - 1,
                player.y.roundToInt(),
                (player.z + 0.25).roundToInt() - 1
            )

            val presetData = TermRegistry.getPreset(currentPresetKey)

            for ((phaseName, classMap) in presetData) {
                val mergedTasks = mutableMapOf<Pair<TaskType, Int>, TaskAssignment>()

                classMap.forEach { (role, tasks) ->
                    if (currentRoleKey == "all" || role == currentRoleKey) {
                        fun merge(ids: List<Int>, type: TaskType) {
                            ids.forEach { id ->
                                val task = mergedTasks.getOrPut(type to id) { TaskAssignment(id, type) }
                                if (!task.roles.contains(role)) task.roles.add(role)
                            }
                        }

                        merge(tasks.term, TaskType.TERMINAL)
                        merge(tasks.lever, TaskType.LEVER)
                        merge(tasks.device, TaskType.DEVICE)
                    }
                }

                mergedTasks.values.forEach { task ->
                    val coord = TermRegistry.getCoord(phaseName, task.type, task.id) ?: return@forEach
                    val isStack = task.roles.size >= 4

                    val labelText = when {
                        task.roles.size > 1 && showTermClass -> {
                            // Create a list of labels for each role and join with newlines
                            // Result will look like:
                            // [ 1 ]
                            // ( Tank )
                            // ( Mage )
                            task.roles.joinToString("") { role ->
                                "\n" + (termLabelMap[role]?.first ?: "")
                            }
                        }
                        task.roles.isNotEmpty() -> {
                            // Single role display
                            "\n" + (termLabelMap[task.roles[0]]?.first ?: "")
                        }
                        else -> ""
                    }

                    val displayColor = when {
                        isStack || (task.roles.size > 1 && currentRoleKey == "all") -> Color.WHITE
                        task.roles.isNotEmpty() -> termLabelMap[task.roles[0]]?.second ?: termColor.toColor()
                        else -> termColor.toColor()
                    }

                    // Prefix handling: L1, D1, or just 1
                    val displayNum = when(task.type) {
                        TaskType.LEVER -> "Lever"
                        TaskType.DEVICE -> "Device"
                        TaskType.TERMINAL -> "${task.id}"
                    }

                    renderTask(event.context, coord, displayNum, labelText, displayColor, playerPos)
                }
            }
        }
    }

    private fun renderTask(
        context: RenderContext,
        coord: TermRegistry.Vec3i,
        displayNum: String,
        label: String,
        color: Color,
        pPos: Triple<Int, Int, Int>
    ) {
        val pdistance = Utils.calcDistance(pPos, Triple(coord.x, coord.y, coord.z))
        if (pdistance >= 900) return

        val text = when {
            hideNumber && showTermClass -> label
            showTermClass -> " \n§l§8[ §f$displayNum §8]$label"
            else ->  " \n§l§8[ §f$displayNum §8]"
        }

        Render3D.renderString(
            text,
            coord.x + 0.5, coord.y + 1.95, coord.z + 0.5,
            bgBox = false,
            increase = pdistance > 13,
            phase = true
        )

        if (highlightTerms) {
            val renderColor = if (classColor) color else termColor.toColor()
            Render3D.outlineBlock(context, coord.toBlockPos(), renderColor, 1.0, false)
        }
    }

    object TermRegistry {
        private val gson = Gson()

        data class RootJson(
            val metadata: JsonObject? = null,
            val presets: Map<String, Map<String, Map<String, TaskLists>>>,
            val coords: Map<String, PhaseCoords>
        )

        data class TaskLists(
            val term: List<Int> = emptyList(),
            val device: List<Int> = emptyList(),
            val lever: List<Int> = emptyList()
        )

        data class PhaseCoords(
            val terms: List<List<Any>> = emptyList(),
            val levers: List<List<Any>> = emptyList(),
            val devices: List<List<Any>> = emptyList()
        )

        data class Vec3i(val x: Int, val y: Int, val z: Int) {
            fun toBlockPos() = BlockPos(x, y, z)
        }

        private var fullData: RootJson? = null
        private val coordCache = mutableMapOf<String, Map<TaskType, Map<Int, Vec3i>>>()

        init { load(client.resourceManager) }

        fun load(resourceManager: ResourceManager) {
            val id = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "dungeons/terms.json")
            try {
                resourceManager.getResource(id).ifPresent { res ->
                    res.open().use { stream ->
                        InputStreamReader(stream).use { reader ->
                            fullData = gson.fromJson(reader, RootJson::class.java)
                        }
                    }
                }

                fullData?.coords?.forEach { (phase, phaseCoords) ->
                    val typeMap = mutableMapOf<TaskType, Map<Int, Vec3i>>()

                    fun parseCoords(rawList: List<List<Any>>): Map<Int, Vec3i> {
                        return rawList.associate { list ->
                            val x = (list[0] as Number).toInt()
                            val y = (list[1] as Number).toInt()
                            val z = (list[2] as Number).toInt()
                            val idNum = (list[3] as Number).toInt()
                            idNum to Vec3i(x, y, z)
                        }
                    }

                    typeMap[TaskType.TERMINAL] = parseCoords(phaseCoords.terms)
                    typeMap[TaskType.LEVER] = parseCoords(phaseCoords.levers)
                    typeMap[TaskType.DEVICE] = parseCoords(phaseCoords.devices)

                    coordCache[phase] = typeMap
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getPreset(name: String): Map<String, Map<String, TaskLists>> = fullData?.presets?.get(name).orEmpty()
        fun getCoord(phase: String, type: TaskType, id: Int): Vec3i? = coordCache[phase]?.get(type)?.get(id)
    }
}