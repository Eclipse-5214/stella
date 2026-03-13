package co.stellarskys.stella.features.secrets

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.utils.Checkmark
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object SecretWaypoints: Feature("secretWaypoints", island = SkyBlockIsland.THE_CATACOMBS) {
    override fun initialize() {
        on<RenderEvent.World.Last> {
            if (Dungeon.inBoss) return@on
            val room = Dungeon.currentRoom ?: return@on
            val data = room.roomData?.secretCoords ?: return@on
            if(room.checkmark == Checkmark.GREEN) return@on

            data.toWaypoints(config, room).forEach { waypoint ->
                waypoint.render()
            }
        }
    }
}