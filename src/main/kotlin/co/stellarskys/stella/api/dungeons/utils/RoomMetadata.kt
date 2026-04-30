package co.stellarskys.stella.api.dungeons.utils

import co.stellarskys.stella.api.config.core.Config
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.api.dungeons.map.Room
import co.stellarskys.stella.utils.config
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.awt.Color

data class RoomMetadata(
    val name: String,
    val type: String,
    val roomID: Int,
    val shape: String? = null,
    val cores: List<Int>,
    val secrets: Int = 0,
    val crypts: Int = 0,
    val trappedChests: Int = 0,
    val reviveStones: Int = 0
)
