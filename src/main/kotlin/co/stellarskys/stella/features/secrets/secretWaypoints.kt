package co.stellarskys.stella.features.secrets

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object secretWaypoints: Feature("secretWaypoints", island = SkyBlockIsland.THE_CATACOMBS) {
    override fun initialize() {
        on<RenderEvent.World.Last> { event ->
            val room = Dungeon.currentRoom ?: return@on
            val data = room.roomData?.secretCoords ?: return@on

            data.toWaypoints(config, room).forEach { waypoint ->
                waypoint.render(event.context)
            }
        }
    }
}