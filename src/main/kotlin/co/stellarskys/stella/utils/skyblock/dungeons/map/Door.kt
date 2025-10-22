package co.stellarskys.stella.utils.skyblock.dungeons.map

import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorState
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.utils.WorldScanUtils

class Door(val worldPos: Pair<Int, Int>, val componentPos: Pair<Int, Int>) {

    var opened: Boolean = false
    var rotation: Int? = null
    var type: DoorType = DoorType.NORMAL
    var state = DoorState.UNDISCOVERED

    fun getPos(): Triple<Int, Int, Int> {
        return Triple(worldPos.first, 69, worldPos.second)
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

    fun check() {
        val (x, y, z) = getPos()
        if (!WorldScanUtils.isChunkLoaded(x, y, z)) return

        val id = WorldUtils.getBlockNumericId(x, y, z)
        opened = (id == 0) && this.type != DoorType.WITHER
    }
}