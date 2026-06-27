package co.stellarskys.stella.api.dungeons.map

import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.score.DungeonScore
import co.stellarskys.stella.api.dungeons.utils.Checkmark
import co.stellarskys.stella.api.dungeons.utils.RoomType
import co.stellarskys.stella.api.dungeons.utils.ScanUtils

object Prediction {
    val floorMin get() = when (Dungeon.floorNumber) {
        0, 1, 2-> 3
        3, 4, 5, 6 -> 4
        else -> 5
    }

    val maxCol: Int get() {
        val roomMin = Dungeon.uniqueRooms.flatMap { it.components }.maxOfOrNull { it.first } ?: 0
        return maxOf(roomMin, floorMin)
    }

    val maxRow: Int get() {
        val roomMin = Dungeon.uniqueRooms.flatMap { it.components }.maxOfOrNull { it.second } ?: 0
        return maxOf(roomMin, floorMin)
    }

    fun predictRoomType(room: Room): Room {
        if (!room.known1x1) return room
        room.predictedTypes.clear()
        if (Dungeon.puzzles.count { it.explored } < DungeonScore.puzzleCount) room.predictedTypes.add(RoomType.PUZZLE)
        if (Dungeon.exploredRooms.none { it.type == RoomType.YELLOW }) room.predictedTypes.add(RoomType.YELLOW)
        if (Dungeon.exploredRooms.none { it.type == RoomType.TRAP }) room.predictedTypes.add(RoomType.TRAP)
        return room
    }

    fun check1x1(room: Room): Boolean {
        if (room.type in setOf(RoomType.ENTRANCE, RoomType.BLOOD, RoomType.UNKNOWN, RoomType.NORMAL)) return false
        if (room.components.size != 1) return false
        val (rmx, rmz) = room.components[0]

        for ((dx, dz) in ScanUtils.mapDirections) {
            val nx = rmx + dx; val nz = rmz + dz;
            if (nx !in 0..maxCol || nz !in 0..maxRow) continue
            val neighbor = Dungeon.getRoomAtComp(nx to nz) ?: return false
            if (neighbor.checkmark == Checkmark.UNDISCOVERED) return false
            if (neighbor.checkmark == Checkmark.UNEXPLORED  && (nx to nz) !in neighbor.visibleComponents) return false
        }
        return true
    }
}