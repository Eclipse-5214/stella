package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.utils.WorldUtils

class Door(val worldPos: Pair<Int, Int>, val componentPos: Pair<Int, Int>) {

    var opened: Boolean = false
    var rotation: Int? = null
    var type: DoorType = DoorType.NORMAL
    var state = DoorState.UNDISCOVERED

    init {
        if (worldPos.first != 0 && worldPos.second != 0) {
            checkType()
        }
    }

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
        if (!isChunkLoaded(x, y, z)) return

        val id = WorldUtils.getBlockNumericId(x, y, z)
        opened = (id == 0) && this.type != DoorType.WITHER
    }

    private fun checkType() {
        val (x, y, z) = getPos()
        if (!isChunkLoaded(x, y, z)) return

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