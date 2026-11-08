package co.stellarskys.stella.features.dungeons.adg

import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.dungeons.utils.Phases
import co.stellarskys.stella.api.handlers.Capsule
import co.stellarskys.stella.utils.render.Render3D
import net.minecraft.core.BlockPos
import java.awt.Color

//? if >= 26.2 {
/*import co.stellarskys.stella.api.zenith.center
*///? }

object WaypointRegistry {
    val waypoints = Capsule("boss_waypoints", WaypointData())

    class WaypointData {
        val phases: MutableMap<Phases.F7, MutableMap<DungeonClass, MutableList<Waypoint>>> = mutableMapOf()

        fun get(phase: Phases.F7, dclass: DungeonClass): List<Waypoint> =
            phases[phase]?.get(dclass).orEmpty()

        fun getAll(): List<Waypoint> =
            phases.values.flatMap { it.values.flatten() }

        fun add(phase: Phases.F7, dclass: DungeonClass, wp: Waypoint) {
            phases.getOrPut(phase) { mutableMapOf() }
                .getOrPut(dclass) { mutableListOf() }
                .add(wp)
        }

        fun remove(phase: Phases.F7, dclass: DungeonClass, wp: Waypoint) {
            phases[phase]?.get(dclass)?.remove(wp)
        }
    }

    data class Waypoint(
        val pos: BlockPos,
        val color: Color,
        val depth: Boolean = true,
        val above: Boolean = false,
        val label: String? = null,
    ) {
        fun render() {
            Render3D.outlineBlock(pos, color, 2f, depth)

            label?.let {
                Render3D.drawText(it, if (above) pos.above().center else pos.center, depth = depth)
            }
        }
    }
}