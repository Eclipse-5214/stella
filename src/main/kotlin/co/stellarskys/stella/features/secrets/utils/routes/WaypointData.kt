package co.stellarskys.stella.features.secrets.utils.routes

import net.minecraft.core.BlockPos
import java.awt.Color

data class WaypointData(
    val pos: BlockPos,
    val type: WaypointType,
    val name: String? = null,
    val color: Color? = null,
    val depth: Boolean? = null
) {
    val label get() = name ?: type.label
    val col get() = color ?: type.color
    val dep get() = depth ?: type.depth
}