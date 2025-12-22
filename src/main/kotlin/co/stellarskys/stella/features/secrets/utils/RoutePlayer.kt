package co.stellarskys.stella.features.secrets.utils

import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.utils.render.RenderContext
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import net.minecraft.world.phys.Vec3
import java.awt.Color

object RoutePlayer {
    fun renderRoute(data: StepData, firstStep: Boolean, context: RenderContext) {
        val room = Dungeon.currentRoom ?: return

        renderLine(data, context, room, firstStep)
        renderWaypoints(data, context, room)
    }

    fun renderRecordingRoute(data: StepData, oldData: StepData?, context: RenderContext) {
        val room = Dungeon.currentRoom ?: return

        renderLine(data, context, room, oldData == null)
        renderWaypoints(data, context, room)

        if (oldData == null) return
        renderLastSecret(oldData, context, room)
    }

    fun renderLine(data: StepData, context: RenderContext, room: Room, firstStep: Boolean) {
        if (data.line.size <= 1) return

        if (firstStep) {
            val startPoint = room.getRealCoord(data.line.first())
            val startPos = Vec3(startPoint.center.x, startPoint.center.y + 1, startPoint.center.z)
            Render3D.renderString("Start!", startPos, bgBox = true)
        }

        data.line.zipWithNext { a, b ->
            val p1 = room.getRealCoord(a)
            val p2 = room.getRealCoord(b)

            Render3D.renderLine(
                p1.center,
                p2.center,
                3f,
                Color.RED,
                context.consumers,
                context.matrixStack
            )
        }
    }

    private fun renderWaypoints(data: StepData, context: RenderContext, room: Room) {
        val firstMine = data.waypoints.firstOrNull { it.type == WaypointType.MINE }

        data.waypoints.forEach { waypoint ->
            val name = waypoint.type != WaypointType.MINE || waypoint == firstMine
            renderWaypoint(waypoint, context, room, name)
        }
    }

    private fun renderLastSecret(data: StepData, context: RenderContext, room: Room) {
        val secret = data.waypoints.firstOrNull { it.type in WaypointType.SECRET } ?: return
        renderWaypoint(secret, context, room)
    }

    private fun renderWaypoint(waypoint: WaypointData, context: RenderContext, room: Room, name: Boolean = true){
        val realPos = room.getRealCoord(waypoint.pos)

        Render3D.outlineBlock(
            context,
            realPos,
            waypoint.type.color,
            3.0,
            true,
        )

        if (name) Render3D.renderString(waypoint.type.label, realPos.center, phase = true)
    }
}