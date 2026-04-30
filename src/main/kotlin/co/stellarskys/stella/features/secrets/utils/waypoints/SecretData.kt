package co.stellarskys.stella.features.secrets.utils.waypoints

import co.stellarskys.stella.api.config.core.Config
import co.stellarskys.stella.api.dungeons.map.Room
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render3D
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import java.awt.Color

data class SecretData (
    val roomID: Int,
    val redstoneKey: List<Coord> = emptyList(),
    val wither: List<Coord> = emptyList(),
    val bat: List<Coord> = emptyList(),
    val item: List<Coord> = emptyList(),
    val chest: List<Coord> = emptyList(),
    val lever: List<Coord> = emptyList()
) {
    fun toWaypoints(config: Config, room: Room): List<Waypoint> = Type.entries.flatMap { getWaypointsForType(config, room, it) }
    fun toWaypoints(config: Config, room: Room, type: Type): List<Waypoint> = getWaypointsForType(config, room, type)

    private fun getWaypointsForType(config: Config, room: Room, type: Type): List<Waypoint> {
        val coords = when (type) {
            Type.REDSTONE_KEY -> redstoneKey
            Type.WITHER -> wither
            Type.CHEST -> chest
            Type.ITEM -> item
            Type.LEVER -> lever
            Type.BAT -> bat
        }

        val state = when (type) {
            Type.REDSTONE_KEY, Type.WITHER, Type.ITEM -> Blocks.SKELETON_SKULL.defaultBlockState()
            Type.CHEST -> Blocks.CHEST.defaultBlockState()
            Type.BAT, Type.LEVER -> null
        }

        val color by config.property<Color>("secretWaypointColor.${type.colorKey}")
        return coords.map { coord -> Waypoint(type.label, color, coord, room, state) }
    }

    data class Coord(
        val x: Int,
        val y: Int,
        val z: Int,
        var collected: Boolean = false
    ) {
        fun toBlockPos(): BlockPos = BlockPos(x, y, z)
    }

    data class Waypoint(
        val label: String,
        val color: Color,
        val position: Coord,
        val room: Room,
        val state: BlockState? = null
    ) {
        val text by config.property<Boolean>("secretWaypoints.text")
        val textScale by config.property<Float>("secretWaypoints.textScale")

        fun render() {
            if (position.collected) return
            val pos = room.getRealCoord(position.toBlockPos())
            val lineWidth = 3f
            Render3D.outlineBlock(pos, color, lineWidth, false, state)
            if (text) Render3D.drawText(label, pos.center.x, pos.center.y, pos.center.z, scale = textScale, depth = false, bgBox = true)
        }
    }

    enum class Type(val label: String, val colorKey: String) {
        REDSTONE_KEY("Redstone Key", "redstonekey"),
        WITHER("Wither", "wither"),
        CHEST("Chest", "chest"),
        ITEM("Item", "item"),
        LEVER("Lever", "lever"),
        BAT("Bat", "bat");

        companion object {
            fun fromLabel(label: String): Type? =
                entries.find { it.label.equals(label, ignoreCase = true) }
        }
    }
}