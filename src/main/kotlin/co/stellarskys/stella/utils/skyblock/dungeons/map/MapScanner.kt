package co.stellarskys.stella.utils.skyblock.dungeons.map

import co.stellarskys.stella.Stella
import co.stellarskys.stella.mixins.accessors.AccessorMapState
import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon.rooms
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.dungeons.utils.Checkmark
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorState
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.MapUtils
import co.stellarskys.stella.utils.skyblock.dungeons.utils.MapUtils.mapX
import co.stellarskys.stella.utils.skyblock.dungeons.utils.MapUtils.mapZ
import co.stellarskys.stella.utils.skyblock.dungeons.utils.MapUtils.yaw
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.ScanUtils
import net.minecraft.world.level.saveddata.maps.MapDecoration
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapItemSavedData
import kotlin.time.Duration


object MapScanner {
    data class RoomClearInfo(
        val time: Duration,
        val room: Room,
        val solo: Boolean
    )

    fun scan(state: MapItemSavedData) {
        val colors = state.colors

        var cx = -1
        for (x in MapUtils.mapCorners.first + MapUtils.mapRoomSize / 2 until 118 step MapUtils.mapGapSize / 2) {
            var cz = -1
            cx++
            for (z in MapUtils.mapCorners.second + MapUtils.mapRoomSize / 2 + 1 until 118 step MapUtils.mapGapSize / 2) {
                cz++
                val idx = x + z * 128
                val center = colors.getOrNull(idx - 1) ?: continue
                val rcolor = colors.getOrNull(idx + 5 + 128 * 4) ?: continue

                if (cx % 2 == 0 && cz % 2 == 0 && rcolor != 0.toByte()) {
                    handleRoom(cx, cz, x, z, idx, center, rcolor, colors)
                } else if ((cx % 2 != 0 || cz % 2 != 0) && center != 0.toByte()) {
                    handleDoor(cx, cz, x, z, idx, center, colors)
                }
            }
        }
    }

    private fun handleRoom(
        cx: Int, cz: Int, x: Int, z: Int, idx: Int,
        center: Byte, rcolor: Byte, colors: ByteArray
    ) {
        val rmx = cx / 2
        val rmz = cz / 2
        val roomIdx = Dungeon.getRoomIdx(rmx to rmz)

        val room = rooms[roomIdx] ?: Room(rmx to rmz).also {
            rooms[roomIdx] = it
            Dungeon.uniqueRooms.add(it)
        }

        // adjacency check
        checkRoomAdjacency(room, cx, cz, x, z, colors)

        // type/height
        if (room.type == RoomType.UNKNOWN && room.height == null) {
            room.loadFromMapColor(rcolor)
        }

        // exploration state
        if (rcolor == 0.toByte()) {
            room.explored = false
            return
        }

        if (center == 119.toByte() || rcolor == 85.toByte()) {
            room.explored = false
            room.checkmark = Checkmark.UNEXPLORED
            Dungeon.discoveredRooms["$rmx/$rmz"] = Dungeon.DiscoveredRoom(x = rmx, z = rmz, room = room)
            return
        }

        // checkmark logic
        updateRoomCheckmark(room, center, rcolor)

        room.explored = true
        Dungeon.discoveredRooms.remove("$rmx/$rmz")
    }

    private fun handleDoor(
        cx: Int, cz: Int, x: Int, z: Int, idx: Int,
        center: Byte, colors: ByteArray
    ) {
        val horiz = listOf(
            colors.getOrNull(idx - 128 - 4) ?: 0,
            colors.getOrNull(idx - 128 + 4) ?: 0
        )
        val vert = listOf(
            colors.getOrNull(idx - 128 * 5) ?: 0,
            colors.getOrNull(idx + 128 * 3) ?: 0
        )

        val isDoor = horiz.all { it == 0.toByte() } || vert.all { it == 0.toByte() }
        if (!isDoor) return

        val comp = cx to cz
        val doorIdx = Dungeon.getDoorIdx(comp)
        val door = Dungeon.getDoorAtIdx(doorIdx)

        val rx = ScanUtils.cornerStart.first + ScanUtils.halfRoomSize + cx * ScanUtils.halfCombinedSize
        val rz = ScanUtils.cornerStart.second + ScanUtils.halfRoomSize + cz * ScanUtils.halfCombinedSize

        val type = when (center.toInt()) {
            119 -> DoorType.WITHER
            18 -> DoorType.BLOOD
            else -> DoorType.NORMAL
        }

        if (door == null) {
            val newDoor = Door(rx to rz, comp).apply {
                rotation = if (cz % 2 == 1) 0 else 1
                setType(type)
                setState(DoorState.DISCOVERED)
            }
            Dungeon.addDoor(newDoor)
        } else {
            door.setState(DoorState.DISCOVERED)
            door.setOpen(center.toInt() !in setOf(119, 18))
        }
    }


    private fun checkRoomAdjacency(room: Room, cx: Int, cz: Int, x: Int, z: Int, colors: ByteArray) {
        for ((dx, dz) in ScanUtils.mapDirections) {
            val doorCx = cx + dx
            val doorCz = cz + dz
            if (doorCx % 2 == 0 && doorCz % 2 == 0) continue

            val doorX = x + dx * MapUtils.mapGapSize / 2
            val doorZ = z + dz * MapUtils.mapGapSize / 2
            val doorIdx = doorX + doorZ * 128
            val center = colors.getOrNull(doorIdx)

            val isGap = center == null || center == 0.toByte()
            val isDoor = if (!isGap) {
                val horiz = listOf(
                    colors.getOrNull(doorIdx - 128 - 4) ?: 0,
                    colors.getOrNull(doorIdx - 128 + 4) ?: 0
                )
                val vert = listOf(
                    colors.getOrNull(doorIdx - 128 * 5) ?: 0,
                    colors.getOrNull(doorIdx + 128 * 3) ?: 0
                )
                horiz.all { it == 0.toByte() } || vert.all { it == 0.toByte() }
            } else false

            if (isGap || isDoor) continue

            val neighborCx = cx + dx * 2
            val neighborCz = cz + dz * 2
            val neighborComp = neighborCx / 2 to neighborCz / 2
            val neighborIdx = Dungeon.getRoomIdx(neighborComp)
            if (neighborIdx !in rooms.indices) continue

            val neighborRoom = rooms[neighborIdx]
            if (neighborRoom == null) {
                room.addComponent(neighborComp)
                rooms[neighborIdx] = room
            } else if (neighborRoom != room && neighborRoom.type != RoomType.ENTRANCE) {
                Dungeon.mergeRooms(neighborRoom, room)
            }
        }
    }

    private fun updateRoomCheckmark(room: Room, center: Byte, rcolor: Byte) {
        var check: Checkmark? = null
        when {
            center == 30.toByte() && rcolor != 30.toByte() -> {
                if (room.checkmark != Checkmark.GREEN) roomCleared(room, Checkmark.GREEN)
                check = Checkmark.GREEN
            }
            center == 34.toByte() -> {
                if (room.checkmark != Checkmark.WHITE) roomCleared(room, Checkmark.WHITE)
                check = Checkmark.WHITE
            }
            rcolor == 18.toByte() && Dungeon.bloodDone -> {
                if (room.checkmark != Checkmark.WHITE) roomCleared(room, Checkmark.WHITE)
                check = Checkmark.WHITE
            }
            center == 18.toByte() && rcolor != 18.toByte() -> check = Checkmark.FAILED
            room.checkmark == Checkmark.UNEXPLORED -> {
                check = Checkmark.NONE
                room.clearTime = TimeUtils.now
            }
        }
        check?.let { room.checkmark = it }
    }

    fun updatePlayers(state: MapItemSavedData) {
        var i = 1

        for ((key, mapDecoration) in (state as AccessorMapState).decorations) {
            var dplayer: DungeonPlayer? = null

            if (mapDecoration.type.value().equals(MapDecorationTypes.FRAME.value())) {
                dplayer = DungeonPlayerManager.players.firstOrNull()
            } else {
                val players = DungeonPlayerManager.players
                while (i < players.size && (dplayer == null || !dplayer.alive)) {
                    dplayer = players[i]
                    i++
                }
            }
            if (dplayer == null) {
                dungeonPlayerError(key, "not found", i - 1, DungeonPlayerManager.players, (state as AccessorMapState).decorations)
                continue
            } else if (!dplayer.alive) {
                dungeonPlayerError(key, "not alive", i - 1, DungeonPlayerManager.players, (state as AccessorMapState).decorations)
                continue
            } else if (dplayer.uuid == null) {
                dungeonPlayerError(key, "has null uuid", i - 1, DungeonPlayerManager.players, (state as AccessorMapState).decorations)
                continue
            }

            if (dplayer.inRender) continue
            Stella.LOGGER.info("Updating player ${dplayer.name} to (${mapDecoration.mapX},${mapDecoration.mapZ}), yaw ${mapDecoration.yaw}")

            dplayer.iconX = clampMap(mapDecoration.mapX.toDouble() - MapUtils.mapCorners.first.toDouble(), 0.0, MapUtils.mapRoomSize.toDouble() * 6 + 20.0, 0.0, ScanUtils.defaultMapSize.first.toDouble())
            dplayer.iconZ = clampMap(mapDecoration.mapZ.toDouble() - MapUtils.mapCorners.second.toDouble(), 0.0, MapUtils.mapRoomSize.toDouble() * 6 + 20.0, 0.0, ScanUtils.defaultMapSize.second.toDouble())
            dplayer.realX = clampMap(dplayer.iconX!!, 0.0, 125.0, -200.0, -10.0)
            dplayer.realZ = clampMap(dplayer.iconZ!!, 0.0, 125.0, -200.0, -10.0)
            dplayer.yaw = mapDecoration.yaw + 180f

            dplayer.currRoom = Dungeon.getRoomAt(dplayer.realX!!.toInt(), dplayer.realZ!!.toInt())
            dplayer.currRoom?.players?.add(dplayer)
        }
    }

    private fun roomCleared(room: Room, check: Checkmark) {
        val players = room.players
        val isGreen = check == Checkmark.GREEN
        val roomKey = room.name ?: "unknown"

        players.forEach { player ->
            val alreadyCleared = player.getWhiteChecks().containsKey(roomKey) || player.getGreenChecks().containsKey(roomKey)

            if (!alreadyCleared) {
                if (players.size == 1) player.minRooms++
                player.maxRooms++
            }

            val colorKey = if (isGreen) "GREEN" else "WHITE"
            val clearedMap = player.clearedRooms[colorKey]

            clearedMap?.putIfAbsent(
                room.name ?: "unknown",
                RoomClearInfo(
                    time = room.clearTime.since,
                    room = room,
                    solo = players.size == 1
                )
            )
        }
    }

    private fun dungeonPlayerError(decorationId: String?, reason: String?, i: Int, dungeonPlayers: Array<DungeonPlayer?>?, mapDecorations: MutableMap<String?, MapDecoration?>?) {
        Stella.LOGGER.error("[Dungeon Map] Dungeon player for map decoration '{}' {}. Player list index (zero-indexed): {}. Player list: {}. Map decorations: {}", decorationId, reason, i, dungeonPlayers.toString(), mapDecorations)
    }

    fun clampMap(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return when {
            n <= inMin -> outMin
            n >= inMax -> outMax
            else -> (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
        }
    }
}