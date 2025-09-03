package co.stellarskys.stella.features.stellanav.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.skyblock.dungeons.Checkmark
import co.stellarskys.stella.utils.skyblock.dungeons.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.RoomType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import net.minecraft.client.resources.IResource
import net.minecraft.client.resources.IResourceManager
import net.minecraft.util.ResourceLocation
import net.minecraft.util.Vec3
import java.awt.Color
import java.io.IOException
import java.io.InputStreamReader

fun oscale(floor: Int?): Float {
    if (floor == null) return 1f
    return when {
        floor == 0 -> 6f / 4f
        floor in 1..3 -> 6f / 5f
        else -> 1f
    }
}

val prevewMap = ResourceLocation(Stella.NAMESPACE, "stellanav/defaultmap")
val greenCheck = ResourceLocation(Stella.NAMESPACE, "stellanav/clear/bloommapgreencheck")
val whiteCheck =ResourceLocation(Stella.NAMESPACE, "stellanav/clear/bloommapwhitecheck")
val failedRoom = ResourceLocation(Stella.NAMESPACE, "stellanav/clear/bloommapfailedroom")
val questionMark = ResourceLocation(Stella.NAMESPACE, "stellanav/clear/bloommapquestionmark")
val GreenMarker = ResourceLocation(Stella.NAMESPACE, "stellanav/markerself")
val WhiteMarker = ResourceLocation(Stella.NAMESPACE, "stellanav/markerother")

fun getCheckmarks(checkmark: Checkmark): ResourceLocation? = when (checkmark) {
    Checkmark.GREEN -> greenCheck
    Checkmark.WHITE -> whiteCheck
    Checkmark.FAILED -> failedRoom
    Checkmark.UNEXPLORED -> questionMark
    else -> null
}

fun getTextColor(check: Checkmark?): String = when (check) {
    null -> "§7"
    Checkmark.WHITE -> "§f"
    Checkmark.GREEN -> "§a"
    Checkmark.FAILED -> "§c"
    else -> "§7"
}

val roomTypes = mapOf(
    63 to "Normal",
    30 to "Entrance",
    74 to "Yellow",
    18 to "Blood",
    66 to "Puzzle",
    62 to "Trap"
)

fun getClassColor(dClass: String?): Color = when (dClass) {
    "Healer"  -> mapConfig.healerColor
    "Mage"    -> mapConfig.mageColor
    "Berserk" -> mapConfig.berzColor
    "Archer"  -> mapConfig.archerColor
    "Tank"    -> mapConfig.tankColor
    else      -> Color(0, 0, 0, 255)
}

val roomTypeColors: Map<RoomType, Color>
    get() = mapOf(
        RoomType.NORMAL to mapConfig.NormalColor,
        RoomType.PUZZLE to mapConfig.PuzzleColor,
        RoomType.TRAP to mapConfig.TrapColor,
        RoomType.YELLOW to mapConfig.MinibossColor,
        RoomType.BLOOD to mapConfig.BloodColor,
        RoomType.FAIRY to mapConfig.FairyColor,
        RoomType.ENTRANCE to mapConfig.EntranceColor,
    )

val doorTypeColors: Map<DoorType, Color>
    get() = mapOf(
        DoorType.NORMAL to mapConfig.NormalDoorColor,
        DoorType.WITHER to mapConfig.WitherDoorColor,
        DoorType.BLOOD to mapConfig.BloodDoorColor,
        DoorType.ENTRANCE to mapConfig.EntranceDoorColor,
    )


data class BossMapData(
    val image: String,
    val bounds: List<List<Double>>,
    val widthInWorld: Int,
    val heightInWorld: Int,
    val topLeftLocation: List<Int>,
    val renderSize: Int? = null
)

object BossMapRegistry {
    private val gson = Gson()
    private val bossMaps = mutableMapOf<String, List<BossMapData>>()

    init {
        val resourceManager = Stella.mc.resourceManager
        load(resourceManager)
    }

    fun load(resourceManager: IResourceManager) {
        val id = ResourceLocation(Stella.NAMESPACE, "dungeons/imagedata.json")
        val resource: IResource? = try {
            resourceManager.getResource(id)
        } catch (e: IOException) {
            println("[BossMaps] error could not find $id")
            null
        }

        if (resource == null) return

        val reader = InputStreamReader(resource.inputStream)
        val type = object : TypeToken<Map<String, List<BossMapData>>>() {}.type
        val parsed = gson.fromJson<Map<String, List<BossMapData>>>(reader, type)

        bossMaps.putAll(parsed)
    }

    fun getBossMap(floor: Int, playerPos: Vec3): BossMapData? {
        val maps = bossMaps[floor.toString()] ?: return null
        return maps.firstOrNull { map ->
            (0..2).all { axis ->
                val min = map.bounds[0][axis]
                val max = map.bounds[1][axis]
                val p = when (axis) {
                    0 -> playerPos.xCoord
                    1 -> playerPos.yCoord
                    else -> playerPos.zCoord
                }
                p in min..max
            }
        }
    }

    fun getAll(): Map<String, List<BossMapData>> = bossMaps
}
