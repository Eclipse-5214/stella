package co.stellarskys.stella.utils.skyblock.dungeons.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.PacketEvent
import co.stellarskys.stella.events.core.TickEvent
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon.inBoss
import co.stellarskys.stella.utils.skyblock.dungeons.map.MapScanner
import co.stellarskys.stella.utils.skyblock.dungeons.utils.ScanUtils.roomDoorCombinedSize
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import net.minecraft.item.FilledMapItem
import net.minecraft.item.map.MapDecoration
import net.minecraft.item.map.MapDecorationTypes
import net.minecraft.item.map.MapState
import net.minecraft.network.packet.s2c.play.MapUpdateS2CPacket
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.KnitPlayer

object MapUtils {
    val MapDecoration.mapX get() = (this.x() + 128) shr 1
    val MapDecoration.mapZ get() = (this.z + 128) shr 1
    val MapDecoration.yaw get() = this.rotation * 22.5f

    var mapCorners = Pair(5, 5)
    var mapRoomSize = 16
    var mapGapSize = 0
    var coordMultiplier = 0.625
    var calibrated = false

    var mapData: MapState? = null
    var guessMapData: MapState? = null

    fun init() {
        EventBus.registerIn<PacketEvent.Received>(SkyBlockIsland.THE_CATACOMBS) { event->
            if (event.packet is MapUpdateS2CPacket && mapData == null) {
                val world = KnitClient.world ?: return@registerIn
                val id = event.packet.mapId.id
                if (id and 1000 == 0) {
                    val guess = FilledMapItem.getMapState(event.packet.mapId, world) ?: return@registerIn
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

    fun getCurrentMapState(): MapState? {
        val stack = KnitPlayer.player?.inventory?.getStack(8) ?: return null
        if (stack.item !is FilledMapItem || !stack.name.string.contains("Magical Map")) return null
        return FilledMapItem.getMapState(stack, KnitClient.world!!)
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

    fun checkBloodDone(state: MapState) {
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