package co.stellarskys.stella.features.secrets.utils.routes

import net.minecraft.core.BlockPos

data class StepData(
    val waypoints: MutableList<WaypointData>,
    val line: MutableList<BlockPos>
)