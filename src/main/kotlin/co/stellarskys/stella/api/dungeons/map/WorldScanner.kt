package co.stellarskys.stella.api.dungeons.map

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.TickEvent
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.api.orbit.Orbit
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.utils.ScanUtils
import co.stellarskys.stella.api.dungeons.utils.WorldScanUtils
import co.stellarskys.stella.api.dungeons.Dungeon.rooms
import co.stellarskys.stella.api.dungeons.players.DungeonPlayer
import co.stellarskys.stella.api.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.api.dungeons.utils.RoomType
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.api.zenith.player
import co.stellarskys.stella.api.zenith.world
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import java.util.UUID

/*
 * Portions of this file are adapted from DungeonScanner.js
 * Original project: https://github.com/DocilElm/tska
 * Author: DocilElm
 *
 * The original work is licensed under the GNU General Public License v3.0.
 * In accordance with the GPL, modifications and derivative portions in this
 * file are also distributed under GPL‑3.0.
 */

object WorldScanner {
    private val availableComponents = LinkedHashSet(ScanUtils.getScanCoords())
    var lastIdx: Int? = null

    fun init() {
        EventBus.on<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {
            val player = player ?: return@on
            checkPlayerState()

            val (x, z) = WorldScanUtils.realCoordToComponent(player.x.toInt(), player.z.toInt())
            val idx = 6 * z + x

            if (idx in 0..35) {
                scan()

                checkRoomState()
                checkDoorState()

                val prevRoom = lastIdx?.let { rooms[it] }
                val currRoom = rooms.getOrNull(idx)

                if (prevRoom != null && currRoom != null && prevRoom != currRoom) {
                    EventBus.post(DungeonEvent.Room.Change(prevRoom, currRoom))
                }

                if (lastIdx == idx) return@on
                lastIdx = idx
                Dungeon.currentRoom = Dungeon.getRoomAt(player.x.toInt(), player.z.toInt())
                val (rmx, rmz) = Dungeon.currentRoom?.components?.firstOrNull() ?: return@on
                Dungeon.discoveredRooms.remove("$rmx/$rmz")
            }
        }
    }

    fun reset() {
        availableComponents.clear()
        availableComponents += ScanUtils.getScanCoords()
        lastIdx = null
    }

    fun scan() {
        val iter = availableComponents.iterator()
        while (iter.hasNext()) {
            val (cx, cz, rxz) = iter.next()
            val (rx, rz) = rxz
            if (!WorldScanUtils.isChunkLoaded(rx, 0, rz)) continue
            val roofHeight = WorldScanUtils.getHighestY(rx, rz) ?: continue
            iter.remove()

            if (cx % 2 == 1 || cz % 2 == 1) {
                addDoor(cx, cz, rx, rz, roofHeight)
                continue
            }

            val room = addRoom(cx, cz, roofHeight)
            checkAdjacent(room, cx, cz, rx, rz, roofHeight)
        }
    }

    // Adds a door
    fun addDoor(cx: Int, cz: Int, rx: Int, rz: Int, roofHeight: Int) {
        if (roofHeight < 85) {
            val comp = cx to cz
            val doorIdx = Dungeon.getDoorIdx(comp)
            val existingDoor = Dungeon.getDoorAtIdx(doorIdx)

            if (existingDoor == null) {
                val door = Door(rx to rz, comp).apply {
                    rotation = if (cz % 2 == 1) 0 else 1
                }
                Dungeon.addDoor(door)
            }
        }
    }

    // Adds a room
    fun addRoom(cx: Int, cz: Int, roofHeight: Int): Room {
        val x = cx / 2
        val z = cz / 2
        val idx = Dungeon.getRoomIdx(x to z)

        var room = rooms[idx]

        if (room != null) {
            if (room.height == null) room.height = roofHeight
            room.scan()
            return room
        } else {
            room = Room(x to z, roofHeight).scan()
            rooms[idx] = room
            Dungeon.uniqueRooms.add(room)
            return room
        }
    }

    // Scan neighbors *before* claiming this room index
    fun checkAdjacent(room: Room, cx: Int, cz: Int, rx: Int, rz: Int, roofHeight: Int){
        val x = cx / 2
        val z = cz / 2

        for ((dx, dz, cxoff, zoff) in ScanUtils.directions) {
            val nx = rx + dx
            val nz = rz + dz
            val blockBelow = Orbit.getBlockNumericId(nx, roofHeight, nz)
            val blockAbove = Orbit.getBlockNumericId(nx, roofHeight + 1, nz)

            if (room.type == RoomType.ENTRANCE && blockBelow != 0) continue
            if (blockBelow == 0 || blockAbove != 0) continue

            val neighborComp = Pair(x + cxoff, z + zoff)
            val neighborIdx = Dungeon.getRoomIdx(neighborComp)
            if (neighborIdx !in rooms.indices) continue

            val neighborRoom = rooms[neighborIdx]

            if (neighborRoom == null) {
                room.addComponent(neighborComp)
                rooms[neighborIdx] = room
            } else if (neighborRoom != room && neighborRoom.type != RoomType.ENTRANCE) {
                Dungeon.mergeRooms(neighborRoom, room)
            }
        }
    }

    fun checkPlayerState() {
        val world = world ?: return

        for (player in DungeonPlayerManager.players) {
            if (player == null) continue

            val entity = world.players().find { it.name.string == player.name }
            val ping = client.connection?.getPlayerInfo(entity?.uuid ?: UUID(0, 0))?.latency ?: -1

            if (entity != null && ping != -1) {
                player.inRender = true
                onPlayerMove(player, entity.x, entity.z, entity.yRot)
            } else {
                player.inRender = false
            }

            if (ping == -1) continue
            val currRoom = player.currRoom ?: continue

            if (currRoom != player.lastRoom) {
                player.lastRoom?.players?.remove(player)
                currRoom.players.add(player)
            }
            player.lastRoom = currRoom
        }
    }

    fun checkRoomState() {
        for (room in rooms) {
            if (room == null || room.rotation != null) continue
            room.findRotation()
        }
    }

    fun checkDoorState() {
        for (door in Dungeon.uniqueDoors) {
            if (door.opened) continue
            door.checkFairy()
            door.check()
        }
    }

    fun onPlayerMove(entity: DungeonPlayer?, x: Double, z: Double, yaw: Float) {
        if (entity == null) return
        entity.inRender = true

        val iconX = Utils.mapRange(x, -200.0, -10.0, 0.0, ScanUtils.defaultMapSize.first.toDouble())
        val iconZ = Utils.mapRange(z, -200.0, -10.0, 0.0, ScanUtils.defaultMapSize.second.toDouble())
        entity.pos.updatePosition(x, z, yaw + 180f, iconX, iconZ)

        if (x in -200.0..-10.0 && z in -200.0..-10.0) {
            val currRoom = Dungeon.getRoomAt(x.toInt(), z.toInt())
            entity.currRoom = currRoom
        }
    }
}