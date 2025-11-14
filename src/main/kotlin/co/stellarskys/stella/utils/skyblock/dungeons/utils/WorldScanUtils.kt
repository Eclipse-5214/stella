package co.stellarskys.stella.utils.skyblock.dungeons.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.utils.WorldUtils
import xyz.meowing.knit.api.KnitClient

object WorldScanUtils {
    val blacklist = setOf(5, 54, 146)

    fun isChunkLoaded(x: Int, y: Int, z: Int): Boolean {
        val world = KnitClient.world ?: return false
        val chunkX = x shr 4
        val chunkZ = z shr 4
        return world.chunkSource.hasChunk(chunkX, chunkZ)
    }

    fun getCore(x: Int, z: Int): Int {
        val sb = StringBuilder(150)
        val height = getHighestY(x, z)?.coerceIn(11..140) ?: 140 .coerceIn(11..140)

        sb.append(CharArray(140 - height) { '0' })
        var bedrock = 0

        for (y in height downTo 12) {
            val id = WorldUtils.checkIfAir(x, y, z)

            if (id == 0 && bedrock >= 2 && y < 69) {
                sb.append(CharArray(y - 11) { '0' })
                break
            }

            if (id == 7) {
                bedrock++
            } else {
                bedrock = 0
                if (id in blacklist) continue
            }
            sb.append(id)
        }
        return sb.toString().hashCode()
    }


    fun getHighestY(x: Int, z: Int): Int? {
        for (y in 255 downTo 0) {
            val id = WorldUtils.getBlockNumericId(x, y, z)
            if (id != 0 && id != 41) return y
        }
        return null
    }

    fun componentToRealCoords(x: Int, z: Int, includeDoors: Boolean = false): Pair<Int, Int> {
        val (x0, z0) = ScanUtils.cornerStart
        val offset = if (includeDoors) ScanUtils.halfCombinedSize else ScanUtils.roomDoorCombinedSize
        return Pair(x0 + ScanUtils.halfRoomSize + offset * x, z0 + ScanUtils.halfRoomSize + offset * z)
    }

    fun realCoordToComponent(x: Int, z: Int, includeDoors: Boolean = false): Pair<Int, Int> {
        val (x0, z0) = ScanUtils.cornerStart
        val size = if (includeDoors) ScanUtils.halfCombinedSize else ScanUtils.roomDoorCombinedSize
        val shift = 4 + ((size - 16) shr 4)
        return Pair(((x - x0 + 0.5).toInt() shr shift), ((z - z0 + 0.5).toInt() shr shift))
    }

    fun rotateCoords(pos: Triple<Int, Int, Int>, degree: Int): Triple<Int, Int, Int> {
        val d = (degree + 360) % 360
        return when (d) {
            0   -> pos
            90  -> Triple(pos.third, pos.second, -pos.first)
            180 -> Triple(-pos.first, pos.second, -pos.third)
            270 -> Triple(-pos.third, pos.second, pos.first)
            else -> pos
        }
    }

    fun getRoomShape(comps: List<Pair<Int, Int>>): String {
        val count = comps.size
        val xs = comps.map { it.first }.toSet()
        val zs = comps.map { it.second }.toSet()

        return when (count) {
            1 -> "1x1"
            2 -> "1x2"
            3 -> if (xs.size == 3 || zs.size == 3) "1x3" else "L"
            4 -> if (xs.size == 1 || zs.size == 1) "1x4" else "2x2"
            else -> "Unknown"
        }
    }
}