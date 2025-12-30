package co.stellarskys.stella.utils.skyblock.dungeons.utils

/*
 * Portions of this file are adapted from DungeonScanner.js
 * Original project: https://github.com/DocilElm/tska
 * Author: DocilElm
 *
 * The original work is licensed under the GNU General Public License v3.0.
 * In accordance with the GPL, modifications and derivative portions in this
 * file are also distributed under GPL‑3.0.
 *
 * Only small parts of the logic (e.g., scanning patterns / algorithms)
 * are derived from the original source; all other code is original work.
 */

object ScanUtils {
    // Dungeon grid constants
    val cornerStart = Pair(-200, -200)
    val cornerEnd   = Pair(-10, -10)

    const val dungeonRoomSize = 31
    const val dungeonDoorSize = 1
    const val roomDoorCombinedSize = dungeonRoomSize + dungeonDoorSize
    const val halfRoomSize = dungeonRoomSize / 2
    const val halfCombinedSize = roomDoorCombinedSize / 2

    val defaultMapSize = Pair(125, 125)

    val directions = listOf(
        listOf(halfCombinedSize, 0, 1, 0),
        listOf(-halfCombinedSize, 0, -1, 0),
        listOf(0, halfCombinedSize, 0, 1),
        listOf(0, -halfCombinedSize, 0, -1)
    )

    val mapDirections = listOf(
        1 to 0,  // East
        -1 to 0, // West
        0 to 1,  // South
        0 to -1  // North
    )

    fun getScanCoords(): List<Triple<Int, Int, Pair<Int, Int>>> {
        val coords = mutableListOf<Triple<Int, Int, Pair<Int, Int>>>()

        for (z in 0..<11) {
            for (x in 0..<11) {
                if (x % 2 == 1 && z % 2 == 1) continue

                val rx = cornerStart.first + halfRoomSize + x * halfCombinedSize
                val rz = cornerStart.second + halfRoomSize + z * halfCombinedSize
                coords += Triple(x, z, Pair(rx, rz))
            }
        }

        return coords
    }
}