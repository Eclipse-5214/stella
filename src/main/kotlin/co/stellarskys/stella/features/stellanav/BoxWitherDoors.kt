package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.api.dungeons.utils.DoorState
import co.stellarskys.stella.api.dungeons.utils.DoorType
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.utils.config
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.awt.Color

@Module
object BoxWitherDoors: Feature("boxWitherDoors", island = SkyBlockIsland.THE_CATACOMBS) {
    var keyObtained = false
    var bloodOpen = false

    val openedDoor = Regex("""^(\w+) opened a WITHER door!$""")
    val bloodOpened = Regex("""^The BLOOD DOOR has been opened!$""")

    val noKey by config.property<Color>("noKeyColor")
    val key by config.property<Color>("keyColor")
    val doorLW by config.property<Int>("doorLineWidth")

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            event matches openedDoor run { keyObtained = false}
            event matches bloodOpened run { bloodOpen = true}
        }

        on<DungeonEvent.KeyPickUp> { keyObtained = true }

        on<RenderEvent.World.Last> {
            if(bloodOpen) return@on

            val color = if (keyObtained) key else noKey

            Dungeon.doors.forEach { door ->
                if (door == null || door.opened) return@forEach
                if (door.state != DoorState.DISCOVERED) return@forEach
                if (door.type !in setOf(DoorType.WITHER, DoorType.BLOOD)) return@forEach

                val (x, y, z) = door.getPos()

                Render3D.drawBox(
                    x.toDouble(), y.toDouble(), z.toDouble(),
                    3.0, 4.0,
                    color, false, doorLW.toFloat()
                )
            }
        }

        on<LocationEvent.ServerChange>{
            bloodOpen = false
            keyObtained = false
        }
    }

    override fun onUnregister() {
        bloodOpen = false
        keyObtained = false
    }
}