package co.stellarskys.stella.utils

import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.world
import net.minecraft.client.multiplayer.PlayerInfo
import net.minecraft.core.BlockPos
import net.minecraft.world.level.GameType
import net.minecraft.world.level.block.state.BlockState

object WorldUtils {
    fun getBlockStateAt(x: Int, y: Int, z: Int): BlockState? {
        val world = world ?: return null
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

    private val tabListComparator: Comparator<PlayerInfo> = compareBy(
        { it.gameMode == GameType.SPECTATOR },
        { it.team?.name ?: "" },
        { it.profile.name.lowercase() }
    )

    @JvmStatic
    val tablist: List<PlayerInfo>
        get() = client.connection
            ?.listedOnlinePlayers
            ?.sortedWith(tabListComparator) ?: emptyList()

    @JvmStatic
    val players: List<PlayerInfo>
        get() = tablist.filter { it.profile.id.version() == 4 }
}