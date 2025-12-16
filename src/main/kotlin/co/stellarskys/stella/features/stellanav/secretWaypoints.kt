package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import net.minecraft.core.BlockPos
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.awt.Color

@Module
object secretWaypoints: Feature("secretWaypoints", island = SkyBlockIsland.THE_CATACOMBS) {
    override fun initialize() {
        register<RenderEvent.World.Last> { event ->
            val room = Dungeon.currentRoom ?: return@register
            val data = room.roomData?.secretCoords ?: return@register

            data.allWithTypes().forEach { (type, coord) ->
                val localPos = BlockPos(coord.x, coord.y, coord.z)
                val realPos = room.getRealCoord(localPos)
                Stella.LOGGER.info("Rendering Waypoint for type $type at $coord")

                Render3D.renderBox(event.context, realPos.x.toDouble(), realPos.y.toDouble(), realPos.z.toDouble(), 1.0, 1.0, Color.GREEN, true)
            }
        }
    }
}

