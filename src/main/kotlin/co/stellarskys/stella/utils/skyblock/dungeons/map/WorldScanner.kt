package co.stellarskys.stella.utils.skyblock.dungeons.map

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.DungeonEvent
import co.stellarskys.stella.events.core.TickEvent
import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.utils.ScanUtils
import co.stellarskys.stella.utils.skyblock.dungeons.utils.WorldScanUtils
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon.rooms
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayer
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomType
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import dev.deftu.omnicore.api.client.world
import java.util.UUID

// Very loosely based of Tska
object WorldScanner {
    val availableComponents = ScanUtils.getScanCoords().toMutableList()
    var lastIdx: Int? = null

    fun init() {
        EventBus.on<TickEvent.Client>(SkyBlockIsland.THE_CATACOMBS) {

            val player = player ?: return@on

            // checking player states
            checkPlayerState()

            val (x, z) = WorldScanUtils.realCoordToComponent(player.x.toInt(), player.z.toInt())
            val idx = 6 * z + x

            // Bounds check
            if (idx < 35) {
                // Scan dungeon
                scan()

                // Rotation & door state updates
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
        if (availableComponents.isEmpty()) return

        for (idx in availableComponents.indices.reversed()) {
            val (cx, cz, rxz) = availableComponents[idx]
            val (rx, rz) = rxz
            if (!WorldScanUtils.isChunkLoaded(rx,0,rz)) continue
            val roofHeight = WorldScanUtils.getHighestY(rx, rz) ?: continue
            availableComponents.removeAt(idx)

            // Door detection
            if (cx % 2 == 1 || cz % 2 == 1) {
                addDoor(cx, cz, rx, rz, roofHeight)
                continue
            }

            val room = addRoom(cx, cz, rx, rz, roofHeight)
            checkAdjacent(room, cx, cz, rx, rz, rx, rz, roofHeight)
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
    fun addRoom(cx: Int, cz: Int, rx: Int, rz: Int, roofHeight: Int): Room {
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
    fun checkAdjacent(room: Room, cx: Int, cz: Int, x: Int, z: Int, rx: Int, rz: Int, roofHeight: Int){
        for ((dx, dz, cxoff, zoff) in ScanUtils.directions.map { it }) {
            val nx = rx + dx
            val nz = rz + dz
            val blockBelow = WorldUtils.getBlockNumericId(nx, roofHeight, nz)
            val blockAbove = WorldUtils.getBlockNumericId(nx, roofHeight + 1, nz)

            if (room.type == RoomType.ENTRANCE && blockBelow != 0) {
                continue
            }
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

        DungeonPlayerManager.players.forEach { player ->
            if (player == null) return@forEach

            val entity = world.players().find { it.name.string == player.name }

            val entry = client.connection?.getPlayerInfo(entity?.uuid ?: UUID(0, 0))
            val ping = entry?.latency ?: -1

            if (ping != -1 && entity != null) {
                player.inRender = true
                onPlayerMove(player, entity.x, entity.z, entity.yRot)
            } else {
                player.inRender = false
            }

            if (ping == -1) return@forEach
            val currRoom = player.currRoom ?: return@forEach

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

        if ( x in -200.0..-10.0 || z in -200.0..-10.0){
            entity.iconX = clampMap(x, -200.0, -10.0, 0.0, ScanUtils.defaultMapSize.first.toDouble())
            entity.iconZ = clampMap(z, -200.0, -10.0, 0.0, ScanUtils.defaultMapSize.second.toDouble())

            val currRoom = Dungeon.getRoomAt(x.toInt(), z.toInt())
            entity.currRoom = currRoom
        }

        entity.realX = x
        entity.realZ = z
        entity.yaw = yaw + 180f
    }

    fun clampMap(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return when {
            n <= inMin -> outMin
            n >= inMax -> outMax
            else -> (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
        }
    }
}