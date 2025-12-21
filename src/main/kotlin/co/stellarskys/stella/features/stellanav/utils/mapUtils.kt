package co.stellarskys.stella.features.stellanav.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.world.phys.Vec3
import java.awt.Color
import java.io.InputStreamReader

fun oscale(floor: Int?): Float {
    if (floor == null) return 1f
    return when {
        floor == 0 -> 6f / 4f
        floor in 1..3 -> 6f / 5f
        else -> 1f
    }
}

val prevewMap = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/defaultmap")
val greenCheck = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapgreencheck")
val whiteCheck =ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapwhitecheck")
val failedRoom = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapfailedroom")
val questionMark = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/clear/bloommapquestionmark")
val GreenMarker = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/markerself")
val WhiteMarker = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "stellanav/markerother")

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
    val width: Int,
    val height: Int,
    val widthInWorld: Int,
    val heightInWorld: Int,
    val topLeftLocation: List<Int>,
    val renderSize: Int? = null
)

object BossMapRegistry {
    private val gson = Gson()
    private val bossMaps = mutableMapOf<String, List<BossMapData>>()

    init {
        val resourceManager = client.resourceManager
        load(resourceManager)
    }

    fun load(resourceManager: ResourceManager) {
        val id = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, "dungeons/imagedata.json")
        val optional = resourceManager.getResource(id)
        val resource = optional.orElse(null) ?: return

        val reader = InputStreamReader(resource.open())
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
                val p = listOf(playerPos.x, playerPos.y, playerPos.z)[axis]
                p in min..max
            }
        }
    }

    fun getAll(): Map<String, List<BossMapData>> = bossMaps
}

fun renderNametag(context: GuiGraphics, name: String, scale: Float) {
    val matrix = context.pose()
    val width = name.width().toFloat()
    val drawX = (-width / 2).toInt()
    val drawY = 0

    val offsets = listOf(
        scale to 0f, -scale to 0f,
        0f to scale, 0f to -scale
    )

    matrix.pushMatrix()
    matrix.scale(scale, scale)
    matrix.translate(0f, 12f)

    if (mapConfig.textShadow) {
        for ((dx, dy) in offsets) {
            matrix.pushMatrix()
            matrix.translate(dx, dy)
            Render2D.drawString(context, "§0$name", drawX, drawY)
            matrix.popMatrix()
        }
    }

    Render2D.drawString(context, name, drawX, drawY)
    matrix.popMatrix()
}

fun typeToColor(type: RoomType): String = when (type) {
    RoomType.NORMAL   -> "7"
    RoomType.PUZZLE   -> "d"
    RoomType.TRAP     -> "6"
    RoomType.YELLOW   -> "e"
    RoomType.BLOOD    -> "c"
    RoomType.FAIRY    -> "d"
    RoomType.RARE     -> "b"
    RoomType.ENTRANCE -> "a"
    RoomType.UNKNOWN  -> "f"
}

fun typeToName(type: RoomType): String = when (type) {
    RoomType.NORMAL   -> "Normal"
    RoomType.PUZZLE   -> "Puzzle"
    RoomType.TRAP     -> "Trap"
    RoomType.YELLOW   -> "Yellow"
    RoomType.BLOOD    -> "Blood"
    RoomType.FAIRY    -> "Fairy"
    RoomType.RARE     -> "Rare"
    RoomType.ENTRANCE -> "Entrance"
    RoomType.UNKNOWN  -> "Unknown"
}
