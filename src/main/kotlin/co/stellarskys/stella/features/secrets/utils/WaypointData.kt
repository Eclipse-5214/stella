package co.stellarskys.stella.features.secrets.utils

import net.minecraft.core.BlockPos
import java.awt.Color

data class WaypointData(
    val pos: BlockPos,
    val type: WaypointType,
    val name: String? = null,
    val color: Color? = null
)