package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorState
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.config
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import java.awt.Color

@Module
object boxWitherDoors: Feature("boxWitherDoors", island = SkyBlockIsland.THE_CATACOMBS) {
    var keyObtained = false
    var bloodOpen = false

    val openedDoor = Regex("""^(\w+) opened a WITHER door!$""")
    val bloodOpened = Regex("""^The BLOOD DOOR has been opened!$""")

    val noKey by config.property<Color>("noKeyColor")
    val key by config.property<Color>("keyColor")
    val doorLW by config.property<Int>("doorLineWidth")

    override fun initialize() {
        on<ChatEvent.Receive> { event ->
            val msg = event.message.stripped

            val doorMatch = openedDoor.find(msg)
            if (doorMatch != null){
                keyObtained = false
                return@on
            }

            val bloodMatch = bloodOpened.find(msg)
            if (bloodMatch != null){
                keyObtained = false
                bloodOpen = true
                return@on
            }
        }

        on<DungeonEvent.KeyPickUp> {
            keyObtained = true
        }

        on<RenderEvent.World.Last> { event ->
            if(bloodOpen) return@on

            val color = if (keyObtained) key else noKey

            Dungeon.doors.forEach { door ->
                if (door == null || door.opened) return@forEach
                if (door.state != DoorState.DISCOVERED) return@forEach
                if (door.type !in setOf(DoorType.WITHER, DoorType.BLOOD)) return@forEach

                val (x, y, z) = door.getPos()

                Render3D.renderBox(
                    event.context,
                    x.toDouble(), y.toDouble(), z.toDouble(),
                    3.0, 4.0,
                    color, true, doorLW.toDouble()
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