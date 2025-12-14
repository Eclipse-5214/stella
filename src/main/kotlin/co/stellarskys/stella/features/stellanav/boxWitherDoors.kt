package co.stellarskys.stella.features.stellanav

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.core.ChatEvent
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.RenderEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.stellanav.utils.mapConfig
import co.stellarskys.stella.utils.render.Render3D
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorState
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DoorType
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.clearCodes
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

@Module
object boxWitherDoors: Feature("boxWitherDoors", island = SkyBlockIsland.THE_CATACOMBS) {
    var keyObtained = false
    var bloodOpen = false

    val openedDoor = Regex("""^(\w+) opened a WITHER door!$""")
    val bloodOpened = Regex("""^The BLOOD DOOR has been opened!$""")

    override fun initialize() {
        register<ChatEvent.Receive> { event ->
            val msg = event.message.string.clearCodes()

            val doorMatch = openedDoor.find(msg)
            if (doorMatch != null){
                keyObtained = false
                return@register
            }

            val bloodMatch = bloodOpened.find(msg)
            if (bloodMatch != null){
                keyObtained = false
                bloodOpen = true
                return@register
            }
        }

        register<DungeonEvent.KeyPickUp> {
            keyObtained = true
        }

        register<RenderEvent.World.Last> { event ->
            if(bloodOpen) return@register

            val color = if (keyObtained) mapConfig.key else mapConfig.noKey

            Dungeon.doors.forEach { door ->
                if (door == null || door.opened) return@forEach
                if (door.state != DoorState.DISCOVERED) return@forEach
                if (door.type !in setOf(DoorType.WITHER, DoorType.BLOOD)) return@forEach

                val (x, y, z) = door.getPos()

                Render3D.renderBox(
                    event.context,
                    x.toDouble(), y.toDouble(), z.toDouble(),
                    3.0, 4.0,
                    color, true, mapConfig.doorLW
                )
            }
        }
    }

    override fun onUnregister() {
        bloodOpen = false
        keyObtained = false
    }
}