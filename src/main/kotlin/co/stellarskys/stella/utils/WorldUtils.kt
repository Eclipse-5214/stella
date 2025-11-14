package co.stellarskys.stella.utils

import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.state.BlockState
import xyz.meowing.knit.api.KnitClient


object WorldUtils {
    fun getBlockStateAt(x: Int, y: Int, z: Int): BlockState? {
        val world = KnitClient.world ?: return null
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