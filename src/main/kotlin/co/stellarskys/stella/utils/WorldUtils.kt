package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos


object WorldUtils {
    fun getBlockStateAt(x: Int, y: Int, z: Int): BlockState? {
        val world = Stella.mc.world ?: return null
        return world.getBlockState(BlockPos(x, y, z))
    }

    fun getBlockNumericId(x: Int, y: Int, z: Int): Int {
        val state = getBlockStateAt(x, y, z)?: return -1
        return LegIDs.getLegacyId(state)
    }

    fun checkIfAir(x: Int, y: Int, z: Int): Int {
        val state = getBlockStateAt(x, y, z)?: return -1
        if (state.isAir) return 0

        return LegIDs.getLegacyId(state)
    }
}