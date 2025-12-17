package co.stellarskys.stella.utils.skyblock.dungeons.map

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomMetadata
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomRegistry
import co.stellarskys.stella.utils.skyblock.dungeons.utils.ScanUtils
import co.stellarskys.stella.utils.skyblock.dungeons.utils.WorldScanUtils
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import co.stellarskys.stella.utils.skyblock.dungeons.utils.WorldScanUtils.rotate
import co.stellarskys.stella.utils.skyblock.dungeons.utils.WorldScanUtils.unrotate
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.level.block.Blocks
import kotlin.properties.Delegates

class Room(
    initialComponent: Pair<Int, Int>,
    var height: Int? = null
) {
    val components = mutableListOf<Pair<Int, Int>>()
    val realComponents = mutableListOf<Pair<Int, Int>>()
    val cores = mutableListOf<Int>()

    var roomData: RoomMetadata? = null
    var explored = false
    val players: MutableSet<DungeonPlayer> = mutableSetOf()
    var checkmark by Delegates.observable(Checkmark.UNDISCOVERED) { _, oldValue, newValue ->
        if (oldValue == newValue || name == "Unknown") return@observable
        val roomPlayers = players.toList()

        EventBus.post(DungeonEvent.Room.StateChange(this, oldValue, newValue, roomPlayers))
    }


    var name: String? = null
    var corner: BlockPos? = null
    var rotation: Int? = null
    var type: RoomType = RoomType.UNKNOWN
    var shape: String = "1x1"

    var secrets: Int = 0
    var secretsFound: Int = 0
    var crypts: Int = 0

    var clearTime = TimeUtils.zero

    init {
        addComponents(listOf(initialComponent))
    }

    fun addComponent(comp: Pair<Int, Int>, update: Boolean = true): Room {
        if (!components.contains(comp)) components += comp
        if (update) update()
        return this
    }

    fun addComponents(comps: List<Pair<Int, Int>>): Room {
        comps.forEach { addComponent(it, update = false) }
        update()
        return this
    }

    fun hasComponent(x: Int, z: Int): Boolean {
        return components.any { it.first == x && it.second == z }
    }

    fun update() {
        components.sortWith(compareBy({ it.first }, { it.second }))
        realComponents.clear()
        realComponents += components.map { WorldScanUtils.componentToRealCoords(it.first, it.second) }
        scan()
        shape = WorldScanUtils.getRoomShape(components)
        corner = null
        rotation = null
    }

    fun scan(): Room {
        for ((x, z) in realComponents) {
            if (height == null) height = WorldScanUtils.getHighestY(x, z)
            val core = WorldScanUtils.getCore(x, z)
            cores += core
            loadFromCore(core)
        }
        return this
    }

    private fun loadFromCore(core: Int): Boolean {
        val data = RoomRegistry.getByCore(core) ?: return false
        loadFromData(data)
        return true
    }

    fun loadFromData(data: RoomMetadata) {
        roomData = data
        name = data.name
        type = ScanUtils.roomTypeMap[data.type.lowercase()] ?: RoomType.NORMAL
        secrets = data.secrets
        crypts = data.crypts

        if (type == RoomType.ENTRANCE) explored = true
    }

    fun loadFromMapColor(color: Byte): Room {
        type = ScanUtils.mapColorToRoomType[color.toInt()] ?: RoomType.UNKNOWN
        when (type) {
            RoomType.BLOOD -> RoomRegistry.getAll().find { it.name == "Blood" }?.let { loadFromData(it) }
            RoomType.ENTRANCE -> RoomRegistry.getAll().find { it.name == "Entrance" }?.let { loadFromData(it) }
            else -> {}
        }
        return this
    }

    fun findRotation(): Room {
        if (height == null) return this

        if (type == RoomType.FAIRY) {
            rotation = 0
            val (x, z) = realComponents.first()
            corner = BlockPos(x - ScanUtils.halfRoomSize, height!!, z - ScanUtils.halfRoomSize)
            return this
        }

        val offsets = listOf(
            Pair(-ScanUtils.halfRoomSize, -ScanUtils.halfRoomSize),
            Pair(ScanUtils.halfRoomSize, -ScanUtils.halfRoomSize),
            Pair(ScanUtils.halfRoomSize, ScanUtils.halfRoomSize),
            Pair(-ScanUtils.halfRoomSize, ScanUtils.halfRoomSize)
        )

        for ((x, z) in realComponents) {
            for ((jdx, offset) in offsets.withIndex()) {
                val (dx, dz) = offset
                val nx = x + dx
                val nz = z + dz

                if (!WorldScanUtils.isChunkLoaded(nx, height!!, nz)) continue
                val state = WorldUtils.getBlockStateAt(nx, height!!, nz) ?: continue
                if (state.`is`(Blocks.BLUE_TERRACOTTA)) {
                    rotation = jdx * 90
                    corner = BlockPos(nx, height!!, nz)
                    return this
                }
            }
        }
        return this
    }

    fun getRoomCoord(pos: BlockPos)   = pos.subtract(Vec3i(corner?.x ?: 0, 0, corner?.z ?: 0)).rotate(rotation ?: 0)
    fun getRealCoord(local: BlockPos) = local.unrotate(rotation ?: 0).offset(Vec3i(corner?.x ?: 0, 0, corner?.z ?: 0))
}