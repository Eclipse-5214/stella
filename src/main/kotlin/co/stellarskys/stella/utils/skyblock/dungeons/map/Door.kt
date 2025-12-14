package co.stellarskys.stella.utils.skyblock.dungeons.map

import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorState
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.WorldScanUtils

class Door(val worldPos: Pair<Int, Int>, val componentPos: Pair<Int, Int>) {

    var opened: Boolean = false
    var fairy: Boolean = false
    var rotation: Int? = null
    var type: DoorType = DoorType.NORMAL
    var state = DoorState.UNDISCOVERED

    fun getPos(): Triple<Int, Int, Int> {
        return Triple(worldPos.first, 69, worldPos.second)
    }

    init {
        if (worldPos.first != 0 && worldPos.second != 0) {
            checkType()
        }
    }

    fun getComp(): Pair<Int, Int> {
        return componentPos
    }

    fun setType(type: DoorType): Door {
        this.type = type
        return this
    }

    fun setState(state: DoorState): Door {
        this.state = state
        return this
    }

    fun setOpen(opened: Boolean): Door {
        this.opened = opened
        return this
    }

    fun check() {
        if (fairy) return

        val (x, y, z) = getPos()
        if (!WorldScanUtils.isChunkLoaded(x, y, z)) return

        val id = WorldUtils.getBlockNumericId(x, y, z)
        opened = (id == 0)
    }

    fun getCanidates(): List<Int> {
        val (cx, cz) = componentPos
        val candidates: List<Pair<Int, Int>> =
            if (cx % 2 == 1) listOf((cx - 1) / 2 to cz / 2, (cx + 1) / 2 to cz / 2)
            else listOf(cx / 2 to (cz - 1) / 2, cx / 2 to (cz + 1) / 2)

        return candidates
            .map { (rx, rz) -> 6 * rz + rx }
            .filter { it in 0..35 }
    }

    fun checkFairy() {
        fairy = getCanidates().any { idx ->
            val room = Dungeon.getRoomAtIdx(idx)
            room != null && room.type == RoomType.FAIRY && !room.explored
        }
    }

    private fun checkType() {
        val (x, y, z) = getPos()
        if (!WorldScanUtils.isChunkLoaded(x, y, z)) return

        val id = WorldUtils.getBlockNumericId(x, y, z)

        if (id == 0 || id == 166) return

        type = when (id) {
            97  -> DoorType.ENTRANCE
            173 -> DoorType.WITHER
            159 -> DoorType.BLOOD
            else -> DoorType.NORMAL
        }

        opened = false
    }
}