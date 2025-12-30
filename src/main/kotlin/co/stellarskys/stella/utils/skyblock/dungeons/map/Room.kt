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
    private val hs = ScanUtils.halfRoomSize
    private val OFFSETS = arrayOf(Pair(-hs, -hs), Pair(hs, -hs), Pair(hs, hs), Pair(-hs, hs))
    private val componentSet = mutableSetOf<Pair<Int, Int>>()
    private var lastUpdatedSize = 0

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
        if (componentSet.add(comp)) {
            components.add(comp)
            if (update) update()
        }
        return this
    }

    fun addComponents(comps: List<Pair<Int, Int>>) = apply {
        if (comps.any { componentSet.add(it).also { added -> if (added) components += it } }) update()
    }

    fun hasComponent(x: Int, z: Int): Boolean = componentSet.contains(x to z)

    fun update() {
        if (components.size == lastUpdatedSize) return
        components.sortWith(compareBy({ it.first }, { it.second }))
        realComponents.clear()
        components.mapTo(realComponents) { WorldScanUtils.componentToRealCoords(it.first, it.second) }

        scan()
        shape = WorldScanUtils.getRoomShape(components)
        corner = null; rotation = null
        lastUpdatedSize = components.size
    }

    fun scan() = apply {
        realComponents.forEach { (x, z) ->
            if (height == null) height = WorldScanUtils.getHighestY(x, z)
            val core = WorldScanUtils.getCore(x, z)
            if (cores.add(core) && roomData == null) {
                RoomRegistry.getByCore(core)?.let { loadFromData(it) }
            }
        }
    }

    fun loadFromData(data: RoomMetadata) {
        roomData = data
        name = data.name
        type = RoomType.fromString(data.type)
        secrets = data.secrets; crypts = data.crypts
        if (type == RoomType.ENTRANCE) explored = true
    }

    fun loadFromMapColor(color: Byte): Room {
        type = RoomType.fromByte(color.toInt())
        when (type) {
            RoomType.BLOOD -> RoomRegistry.getAll().find { it.name == "Blood" }?.let { loadFromData(it) }
            RoomType.ENTRANCE -> RoomRegistry.getAll().find { it.name == "Entrance" }?.let { loadFromData(it) }
            else -> {}
        }
        return this
    }

    fun findRotation(): Room {
        if (rotation != null || height == null) return this
        val h = height!!

        if (type == RoomType.FAIRY) {
            rotation = 0
            val (x, z) = realComponents.first()
            corner = BlockPos(x - hs, h, z - hs)
            return this
        }

        val targets = if (components.size > 1) listOf(realComponents.first(), realComponents.last()) else realComponents

        for ((x, z) in targets) {
            OFFSETS.forEachIndexed { i, (dx, dz) ->
                val nx = x + dx; val nz = z + dz
                if (WorldScanUtils.isChunkLoaded(nx, h, nz) &&
                    WorldUtils.getBlockStateAt(nx, h, nz)?.`is`(Blocks.BLUE_TERRACOTTA) == true) {
                    rotation = i * 90
                    corner = BlockPos(nx, h, nz)
                    return@findRotation this
                }
            }
        }
        return this
    }

    fun getRoomCoord(pos: BlockPos)   = pos.subtract(Vec3i(corner?.x ?: 0, 0, corner?.z ?: 0)).rotate(rotation ?: 0)
    fun getRealCoord(local: BlockPos) = local.unrotate(rotation ?: 0).offset(Vec3i(corner?.x ?: 0, 0, corner?.z ?: 0))
}