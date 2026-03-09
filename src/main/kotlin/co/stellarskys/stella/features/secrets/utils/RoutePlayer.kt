package co.stellarskys.stella.features.secrets.utils

import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import net.minecraft.world.phys.Vec3
import java.awt.Color

object RoutePlayer {
    val text by config.property<Boolean>("secretRoutes.text")
    val textScale by config.property<Float>("secretRoutes.textScale")

    fun renderRoute(data: StepData, firstStep: Boolean) {
        val room = Dungeon.currentRoom ?: return

        renderLine(data, room, firstStep)
        renderWaypoints(data, room)
    }

    fun renderRecordingRoute(data: StepData, oldData: StepData?) {
        val room = Dungeon.currentRoom ?: return

        renderLine(data, room, oldData == null)
        renderWaypoints(data, room)

        if (oldData == null) return
        renderLastSecret(oldData, room)
    }

    fun renderLine(data: StepData, room: Room, firstStep: Boolean) {
        if (data.line.size <= 1) return

        if (firstStep) {
            val startPoint = room.getRealCoord(data.line.first())
            val startPos = Vec3(startPoint.center.x, startPoint.center.y + 1, startPoint.center.z)
            Render3D.drawText("Start!", startPos, bgBox = true)
        }

        data.line.zipWithNext { a, b ->
            val p1 = room.getRealCoord(a)
            val p2 = room.getRealCoord(b)

            Render3D.drawLine(
                p1.center,
                p2.center,
                3f,
                Color.RED,
            )
        }
    }

    private fun renderWaypoints(data: StepData, room: Room) {
        val firstMine = data.waypoints.firstOrNull { it.type == WaypointType.MINE }

        data.waypoints.forEach { waypoint ->
            val name = waypoint.type != WaypointType.MINE || waypoint == firstMine
            renderWaypoint(waypoint, room, name)
        }
    }

    private fun renderLastSecret(data: StepData, room: Room) {
        val secret = data.waypoints.firstOrNull { it.type in WaypointType.SECRET } ?: return
        renderWaypoint(secret, room)
    }

    private fun renderWaypoint(waypoint: WaypointData, room: Room, name: Boolean = true){
        val realPos = room.getRealCoord(waypoint.pos)
        Render3D.outlineBlock(realPos, waypoint.type.color, 3f, true)
        if (name && text) Render3D.drawText(waypoint.type.label, realPos.center, scale = textScale, depth = false)
    }
}