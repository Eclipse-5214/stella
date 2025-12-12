package co.stellarskys.stella.utils.skyblock.dungeons.utils

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.PacketEvent
import co.stellarskys.stella.events.core.TickEvent
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon.inBoss
import co.stellarskys.stella.utils.skyblock.dungeons.map.MapScanner
import co.stellarskys.stella.utils.skyblock.dungeons.utils.ScanUtils.roomDoorCombinedSize
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.world
import net.minecraft.network.protocol.game.ClientboundMapItemDataPacket
import net.minecraft.world.item.MapItem
import net.minecraft.world.level.saveddata.maps.MapDecoration
import net.minecraft.world.level.saveddata.maps.MapDecorationTypes
import net.minecraft.world.level.saveddata.maps.MapItemSavedData

object MapUtils {
    val MapDecoration.mapX get() = (this.x() + 128) shr 1
    val MapDecoration.mapZ get() = (this.y() + 128) shr 1
    val MapDecoration.yaw get() = this.rot * 22.5f

    var mapCorners = Pair(5, 5)
    var mapRoomSize = 16
    var mapGapSize = 0
    var coordMultiplier = 0.625
    var calibrated = false

    var mapData: MapItemSavedData? = null
    var guessMapData: MapItemSavedData? = null

    fun init() {
        EventBus.registerIn<PacketEvent.Received>(SkyBlockIsland.THE_CATACOMBS) { event->
            if (event.packet is ClientboundMapItemDataPacket && mapData == null) {
                val world = world ?: return@registerIn
                val id = event.packet.mapId.id
                if (id and 1000 == 0) {
                    val guess = MapItem.getSavedData(event.packet.mapId, world) ?: return@registerIn
                    if(guess.decorations.any {it.type == MapDecorationTypes.FRAME }) {
                        guessMapData = guess
                    }
                }
            }
        }

        EventBus.registerIn<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            if (!calibrated) {
                if (mapData == null) {
                    mapData = getCurrentMapState()
                }

                calibrated = calibrateDungeonMap()
            } else if (!inBoss) {
                (mapData ?: guessMapData)?.let {
                    MapScanner.updatePlayers(it)
                    MapScanner.scan(it)
                    checkBloodDone(it)
                }
            }
        }
    }

    fun getCurrentMapState(): MapItemSavedData? {
        val stack = player?.inventory?.getItem(8) ?: return null
        if (stack.item !is MapItem || !stack.hoverName.string.contains("Magical Map")) return null
        return MapItem.getSavedData(stack, world!!)
    }

    fun calibrateDungeonMap(): Boolean {
        val mapState = getCurrentMapState() ?: return false
        val entranceInfo = findEntranceCorner(mapState.colors) ?: return false

        val (startIndex, size) = entranceInfo
        mapRoomSize = size
        mapGapSize = mapRoomSize + 4 // compute gap size from room width

        var x = (startIndex % 128) % mapGapSize
        var z = (startIndex / 128) % mapGapSize

        val floor = Dungeon.floorNumber?: return false
        if (floor in listOf(0, 1)) x += mapGapSize
        if (floor == 0) z += mapGapSize

        mapCorners = x to z
        coordMultiplier = mapGapSize / roomDoorCombinedSize.toDouble()

        return true
    }

    fun findEntranceCorner(colors: ByteArray): Pair<Int, Int>? {
        for (i in colors.indices) {
            if (colors[i] != 30.toByte()) continue

            // Check horizontal 15-block chain
            if (i + 15 < colors.size && colors[i + 15] == 30.toByte()) {
                // Check vertical 15-block chain
                if (i + 128 * 15 < colors.size && colors[i + 128 * 15] == 30.toByte()) {
                    var length = 0
                    while (i + length < colors.size && colors[i + length] == 30.toByte()) {
                        length++
                    }
                    return Pair(i, length)
                }
            }
        }
        return null
    }

    fun checkBloodDone(state: MapItemSavedData) {
        if (Dungeon.bloodClear) return

        val startX = mapCorners.first + (mapRoomSize / 2)
        val startY = mapCorners.second + (mapRoomSize / 2) + 1

        for (x in startX until 118 step (mapGapSize / 2)) {
            for (y in startY until 118 step (mapGapSize / 2)) {
                val i = x + y * 128
                if (state.colors.getOrNull(i) == null) continue

                val center = state.colors[i - 1]
                val roomColor = state.colors.getOrNull(i + 5 + 128 * 4) ?: continue

                if (roomColor != 18.toByte()) continue
                if (center != 30.toByte()) continue
                Dungeon.bloodClear = true
            }
        }
    }

    fun reset() {
        mapCorners = Pair(5, 5)
        mapRoomSize = 16
        mapGapSize = 0
        coordMultiplier = 0.625
        calibrated = false
        mapData = null
        guessMapData = null
    }
}