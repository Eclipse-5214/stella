package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos

object WorldUtils {
    fun getBlockStateAt(x: Int, y: Int, z: Int): IBlockState? {
        val world = Stella.mc.theWorld ?: return null
        return world.getBlockState(BlockPos(x, y, z))
    }

    fun getBlockNumericId(x: Int, y: Int, z: Int): Int {
        val state = getBlockStateAt(x, y, z) ?: return -1
        return Block.getIdFromBlock(state.block)
    }

    fun checkIfAir(x: Int, y: Int, z: Int): Int {
        val state = getBlockStateAt(x, y, z) ?: return -1
        if (state.block.isAir(Stella.mc.theWorld, BlockPos(x, y, z))) return 0
        return Block.getIdFromBlock(state.block)
    }
}
