package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.AreaEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.CommandUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.WorldUtils
import co.stellarskys.stella.utils.skyblock.LocationUtils
//#if MC >= 1.21.5
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource
import net.minecraft.client.util.DefaultSkinHelper
import net.minecraft.entity.player.PlayerModelPart
import net.minecraft.item.map.MapState
//#elseif MC == 1.8.9
//$$ import co.stellarskys.stella.utils.CompatHelpers.*
//$$ import net.minecraft.client.entity.AbstractClientPlayer
//$$ import net.minecraft.client.renderer.entity.layers.LayerRenderer
//$$ import net.minecraft.entity.player.EnumPlayerModelParts
//$$ import net.minecraft.client.resources.DefaultPlayerSkin
//$$ import net.minecraft.world.storage.MapData
//#endif

import java.util.UUID

object DungeonScanner {
    val availableComponents = getScanCoords().toMutableList()
    val rooms = Array<Room?>(36) { null }
    val doors = Array<Door?>(60) { null }
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>()
    val discoveredRooms = mutableMapOf<String, DiscoveredRoom>()
    val players = mutableListOf<DungeonPlayer>()

    var currentRoom: Room? = null
    var lastIdx: Int? = null

    // TODO: room enter/leave listener
    // val onRoomEnter = mutableListOf<(Room?) -> Unit>()
    // val onRoomLeave = mutableListOf<(Room?, Room?) -> Unit>()

    val tickRegister: EventBus.EventCall = EventBus.register<TickEvent.Client>({
        val player = Stella.mc.player ?: return@register
        if (LocationUtils.area != "catacombs") return@register

        // checking player states
        checkPlayerState()


        val (x, z) = realCoordToComponent(player.x.toInt(), player.z.toInt())
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

            /*
            if (lastIdx != null && lastIdx != idx && prevRoom?.name != currRoom?.name) {
                _roomLeaveListener.forEach { it(currRoom, prevRoom) }
            }
            */

            if (lastIdx == idx) return@register


            if (prevRoom?.name != currRoom?.name) {
                // _roomEnterListener.forEach { it(currRoom) }
                println("Entered Room! ${currentRoom?.name}")
            }

            lastIdx = idx
            currentRoom = getRoomAt(player.x.toInt(), player.z.toInt())
            currentRoom?.explored = true
            val (rmx, rmz) = currentRoom?.components?.firstOrNull() ?: return@register
            discoveredRooms.remove("$rmx/$rmz")
        }
    },false)

    data class RoomClearInfo(
        val time: Int,
        val room: Room,
        val solo: Boolean
    )

    data class DiscoveredRoom(
        val x: Int,
        val z: Int,
        val room: Room,
    )

    fun init() {
        println("[Dungeon Scanner] Initilizing")
        EventBus.register<AreaEvent.Main> ({
            TickUtils.schedule(2) {
                if (LocationUtils.area != "catacombs") {
                    println("resetting")
                    reset()
                }
                println("reregistering")
                tickRegister.register()
            }
        })
    }

    fun onPlayerMove(entity: DungeonPlayer?, x: Double, z: Double, yaw: Float) {
        if (entity == null) return
        entity.inRender = true

        if ( x in -200.0..-10.0 || z in -200.0..-10.0){
            entity.iconX = clampMap(x, -200.0, -10.0, 0.0, defaultMapSize.first.toDouble())
            entity.iconZ = clampMap(z, -200.0, -10.0, 0.0, defaultMapSize.second.toDouble())

            val currRoom = getRoomAt(x.toInt(), z.toInt())
            entity.currentRoom = currRoom
        }

        entity.realX = x
        entity.realZ = z
        entity.rotation = yaw + 180f
    }


    fun reset() {
        tickRegister.unregister()
        availableComponents.clear()
        availableComponents += getScanCoords()
        rooms.fill(null)
        doors.fill(null)
        uniqueRooms.clear()
        uniqueDoors.clear()
        discoveredRooms.clear()
        currentRoom = null
        lastIdx = null
        players.clear()
    }

    fun scan() {
        if (availableComponents.isEmpty()) return

        for (idx in availableComponents.indices.reversed()) {
            val (cx, cz, rxz) = availableComponents[idx]
            val (rx, rz) = rxz
            if (!isChunkLoaded(rx,0,rz)) continue
            val roofHeight = getHighestY(rx, rz) ?: continue
            availableComponents.removeAt(idx)

            // Door detection
            if (cx % 2 == 1 || cz % 2 == 1) {
                if (roofHeight < 85) {
                    val comp = cx to cz
                    val doorIdx = getDoorIdx(comp)
                    val existingDoor = getDoorAtIdx(doorIdx)

                    if (existingDoor == null) {
                        val door = Door(rx to rz, comp).apply {
                            rotation = if (cz % 2 == 1) 0 else 1
                        }
                        addDoor(door)
                    }
                }
                continue
            }

            val x = cx / 2
            val z = cz / 2
            val idx = getRoomIdx(x to z)

            var room = rooms[idx]

            if (room != null) {
                if (room.height == null) room.height = roofHeight
                room.scan()
            } else {
                room = Room(x to z, roofHeight).scan()
                rooms[idx] = room
                uniqueRooms.add(room)
            }

            // Scan neighbors *before* claiming this room index
            for ((dx, dz, cxoff, zoff) in directions.map { it }) {
                val nx = rx + dx
                val nz = rz + dz
                val blockBelow = WorldUtils.getBlockNumericId(nx, roofHeight, nz)
                val blockAbove = WorldUtils.getBlockNumericId(nx, roofHeight + 1, nz)

                if (room.type == RoomType.ENTRANCE && blockBelow != 0) {
                    continue
                }
                if (blockBelow == 0 || blockAbove != 0) continue

                val neighborComp = Pair(x + cxoff, z + zoff)
                val neighborIdx = getRoomIdx(neighborComp)
                if (neighborIdx !in rooms.indices) continue

                val neighborRoom = rooms[neighborIdx]

                if (neighborRoom == null) {
                    room.addComponent(neighborComp)
                    rooms[neighborIdx] = room
                } else if (neighborRoom != room && neighborRoom.type != RoomType.ENTRANCE) {
                    mergeRooms(neighborRoom, room)
                }
            }
        }
    }

    fun getRoomAtIdx(idx: Int): Room? {
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomAtComp(comp: Pair<Int, Int>): Room? {
        val idx = getRoomIdx(comp)
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomAt(x: Int, z: Int): Room? {
        val comp = realCoordToComponent(x, z)
        val idx = getRoomIdx(comp)
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomIdx(comp: Pair<Int, Int>): Int = 6 * comp.second + comp.first

    fun getDoorIdx(comp: Pair<Int, Int>): Int {
        val base = ((comp.first - 1) shr 1) + 6 * comp.second
        return base - (base / 12)
    }

    fun getDoorAtIdx(idx: Int): Door? {
        return if (idx in doors.indices) doors[idx] else null
    }

    fun getDoorAtComp(comp: Pair<Int, Int>): Door? {
        val idx = getDoorIdx(comp)
        return getDoorAtIdx(idx)
    }

    fun getDoorAt(x: Int, z: Int): Door? {
        val comp = realCoordToComponent(x, z)
        return getDoorAtComp(comp)
    }

    fun addDoor(door: Door) {
        val idx = getDoorIdx(door.getComp())
        if (idx !in doors.indices) return

        doors[idx] = door
        uniqueDoors += door
    }

    private fun mergeRooms(room1: Room, room2: Room) {
        uniqueRooms.remove(room2)
        for (comp in room2.components) {
            if (!room1.hasComponent(comp.first, comp.second)) {
                room1.addComponent(comp, update = false)
            }
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = room1
        }
        uniqueRooms += room1
        room1.update()
    }

    fun removeRoom(room: Room) {
        for (comp in room.components) {
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = null
        }
    }

    fun checkRoomState() {
        for (room in rooms) {
            if (room == null || room.rotation != null) continue
            room.findRotation()
        }
    }

    fun checkDoorState() {
        for (door in uniqueDoors) {
            if (door.opened) continue
            door.check()
        }
    }

    fun roomCleared(room: Room, check: Checkmark) {
        val players = room.players
        val isGreen = check == Checkmark.GREEN

        players.forEach {
            val v = it

            val colorKey = if (isGreen) "GREEN" else "WHITE"
            val clearedMap = v.clearedRooms[colorKey]

            clearedMap?.putIfAbsent(
                room.name ?: "unknown",
                RoomClearInfo(
                    time = Dungeon.dungeonSeconds,
                    room = room,
                    solo = players.size == 1
                )
            )
        }
    }

    fun checkPlayerState() {
        val world = Stella.mc.world ?: return

        // Sync missing players before comparison
        for (v in Dungeon.partyMembers) {
            val isAlreadyTracked = players.any { it.name == v }

            //#if MC >= 1.21.5
            val playerObj = world.players.firstOrNull { it.name.string == v }
            val entry = Stella.mc.networkHandler?.getPlayerListEntry(playerObj?.uuid ?: UUID(0, 0))
            val ping = entry?.latency ?: -1
            val skinTexture = entry?.skinTextures?.comp_1626 ?: DefaultSkinHelper.getTexture()
            //#elseif MC == 1.8.9
            //$$ val playerObj = world.playerEntities.firstOrNull { it.name == v }
            //$$ val entry = Stella.mc.netHandler?.getPlayerInfo(v)
            //$$ val ping = entry?.responseTime ?: -1
            //$$ val skinTexture = entry?.locationSkin ?: DefaultPlayerSkin.getDefaultSkinLegacy()
            //#endif

            if (isAlreadyTracked || ping == -1) continue

            players.add(DungeonPlayer(v).apply { skin = skinTexture })
        }

        if (players.size != Dungeon.partyMembers.size) return

        for (v in players) {
            //#if MC >= 1.21.5
            val p = world.players.find { it.name.string == v.name }
            val hasHat = p?.isPartVisible(PlayerModelPart.HAT) ?: v.hat
            val ping = Stella.mc.networkHandler?.getPlayerListEntry(p?.uuid ?: UUID(0, 0))?.latency ?: -1
            //#elseif MC == 1.8.9
            //$$ val p = world.playerEntities.firstOrNull { it.name == v.name }
            //$$ val hasHat = p?.isWearing(EnumPlayerModelParts.HAT) ?: v.hat
            //$$ val ping = Stella.mc.netHandler?.getPlayerInfo(v.name)?.responseTime ?: -1
            //#endif

            v.hat = hasHat

            if (ping != -1 && p != null) {
                v.inRender = true
                onPlayerMove(v, p.x, p.z, p.yaw)
            } else {
                v.inRender = false
            }

            if (ping == -1) continue

            val currRoom = v.currentRoom ?: continue

            if (currRoom != v.lastRoom) {
                v.lastRoom?.players?.remove(v)
                currRoom.players.add(v)
            }

            v.visitedRooms.putIfAbsent(currRoom, 0)
            v.lastRoomCheck?.let {
                val timeSpent = System.currentTimeMillis() - it
                v.visitedRooms[currRoom] = (v.visitedRooms[currRoom] ?: 0) + timeSpent
            }

            v.lastRoomCheck = System.currentTimeMillis()
            v.lastRoom = currRoom
        }
    }

    fun getExploredRooms(): List<Room> {
        return rooms.filterNotNull().filter { it.explored }.distinct()
    }

    // MapStuff
    fun updatePlayersFromMap(){
        for ((k, v) in Dungeon.icons) {
            val player = players.find { it.name == v.player }
            if (player == null || player.inRender) continue

            player.iconX = clampMap(v.x.toDouble() - Dungeon.mapCorners.first.toDouble(), 0.0, Dungeon.mapRoomSize.toDouble() * 6 + 20.0, 0.0, defaultMapSize.first.toDouble())
            player.iconZ = clampMap(v.y.toDouble() - Dungeon.mapCorners.second.toDouble(), 0.0, Dungeon.mapRoomSize.toDouble() * 6 + 20.0, 0.0, defaultMapSize.second.toDouble())
            player.realX = clampMap(player.iconX!!, 0.0, 125.0, -200.0, -10.0)
            player.realZ = clampMap(player.iconZ!!, 0.0, 125.0, -200.0, -10.0)
            player.rotation = v.yaw
            player.currentRoom = getRoomAt(player.realX!!.toInt(), player.realZ!!.toInt())
            player.currentRoom?.players?.add(player)
        }
    }

    fun scanFromMap(
        //#if MC >= 1.21.5
        state: MapState
        //#elseif MC == 1.8.9
        //$$ state: MapData
        //#endif
    ) {
        val colors = state.colors

        var cx = -1
        for (x in Dungeon.mapCorners.first + Dungeon.mapRoomSize / 2 until 118 step Dungeon.mapGapSize / 2) {
            var cz = -1
            cx++
            for (z in Dungeon.mapCorners.second + Dungeon.mapRoomSize / 2 + 1 until 118 step Dungeon.mapGapSize / 2) {
                cz++
                val idx = x + z * 128
                val center = colors.getOrNull(idx - 1) ?: continue
                val rcolor = colors.getOrNull(idx + 5 + 128 * 4) ?: continue

                // Room center (even/even grid)
                if (cx % 2 == 0 && cz % 2 == 0 && rcolor != 0.toByte()) {
                    val rmx = cx / 2
                    val rmz = cz / 2
                    val roomIdx = getRoomIdx(rmx to rmz)
                    val room = rooms[roomIdx] ?: Room(rmx to rmz).also {newRoom ->
                        rooms[roomIdx] = newRoom
                        uniqueRooms.add(newRoom)

                        for ((dx, dz) in mapDirections) {
                            val doorCx = cx + dx
                            val doorCz = cz + dz

                            // Only scan odd coordinates (door space)
                            if (doorCx % 2 == 0 && doorCz % 2 == 0) {
                                println("Not odd")
                                continue
                            }

                            val doorX = x + dx * Dungeon.mapGapSize / 2
                            val doorZ = z + dz * Dungeon.mapGapSize / 2
                            val doorIdx = doorX + doorZ * 128
                            val center = colors.getOrNull(doorIdx)

                            // If there's a pixel, and it's a door, skip merging
                            val isGap = center == null || center == 0.toByte()
                            val isDoor = if (!isGap) {
                                val horiz = listOf(
                                    colors.getOrNull(doorIdx - 128 - 4) ?: 0,
                                    colors.getOrNull(doorIdx - 128 + 4) ?: 0
                                )
                                val vert = listOf(
                                    colors.getOrNull(doorIdx - 128 * 5) ?: 0,
                                    colors.getOrNull(doorIdx + 128 * 3) ?: 0
                                )
                                horiz.all { it == 0.toByte() } || vert.all { it == 0.toByte() }
                            } else false

                            if (isGap || isDoor) continue // skip if there's a gap or a door

                            val neighborCx = cx + dx * 2
                            val neighborCz = cz + dz * 2
                            val neighborComp = neighborCx / 2 to neighborCz / 2
                            val neighborIdx = getRoomIdx(neighborComp)
                            if (neighborIdx !in rooms.indices) continue

                            val neighborRoom = rooms[neighborIdx]
                            if (neighborRoom == null) {
                                newRoom.addComponent(neighborComp)
                                rooms[neighborIdx] = newRoom
                                println("Room at $rmx/$rmz is taking over comp at ${neighborComp.first}/${neighborComp.second}")
                            } else if (neighborRoom != newRoom && neighborRoom.type != RoomType.ENTRANCE) {
                                mergeRooms(neighborRoom, newRoom)
                                println("Merged room at $rmx/$rmz with neighbor at ${neighborComp.first}/${neighborComp.second}")
                            }
                        }
                    }

                    if (room.type == RoomType.UNKNOWN && room.height == null) {
                        room.loadFromMapColor(rcolor)
                    }

                    if (rcolor == 0.toByte()) {
                        room.explored = false
                        continue
                    }

                    if (center == 119.toByte() || rcolor == 85.toByte()) {
                        room.explored = false
                        room.checkmark = Checkmark.UNEXPLORED
                        discoveredRooms["$rmx/$rmz"] = DiscoveredRoom(x = rmx, z = rmz, room = room)
                        continue
                    }

                    // Checkmark logic
                    var check: Checkmark? = null
                    when {
                        center == 30.toByte() && rcolor != 30.toByte() -> {
                            if (room.checkmark != Checkmark.GREEN) roomCleared(room, Checkmark.GREEN)
                            check = Checkmark.GREEN
                        }
                        center == 34.toByte() -> {
                            if (room.checkmark != Checkmark.WHITE) roomCleared(room, Checkmark.WHITE)
                            check = Checkmark.WHITE
                        }
                        rcolor == 18.toByte() && Dungeon.bloodOpen -> {
                            if (room.checkmark != Checkmark.WHITE) roomCleared(room, Checkmark.WHITE)
                            check = Checkmark.WHITE
                        }
                        center == 18.toByte() && rcolor != 18.toByte() -> check = Checkmark.FAILED
                        room.checkmark == Checkmark.UNEXPLORED -> check = Checkmark.NONE
                    }

                    check?.let { room.checkmark = it }
                    room.explored = true
                    discoveredRooms.remove("$rmx/$rmz")
                    continue
                }

                // Door detection (odd coordinate pairing)
                if ((cx % 2 != 0 || cz % 2 != 0) && center != 0.toByte()) {
                    val horiz = listOf(
                        colors.getOrNull(idx - 128 - 4) ?: 0,
                        colors.getOrNull(idx - 128 + 4) ?: 0
                    )
                    val vert = listOf(
                        colors.getOrNull(idx - 128 * 5) ?: 0,
                        colors.getOrNull(idx + 128 * 3) ?: 0
                    )

                    val isDoor = horiz.all { it == 0.toByte() } || vert.all { it == 0.toByte() }
                    if (!isDoor) continue // skip false doors

                    val comp = cx to cz
                    val doorIdx = getDoorIdx(comp)
                    val door = getDoorAtIdx(doorIdx)

                    val rx = cornerStart.first + halfRoomSize + cx * halfCombinedSize
                    val rz = cornerStart.second + halfRoomSize + cz * halfCombinedSize

                    val type = when (center.toInt()) {
                        119 -> DoorType.WITHER
                        18 -> DoorType.BLOOD
                        else -> DoorType.NORMAL
                    }

                    if (door == null) {
                        val newDoor = Door(rx to rz, comp).apply {
                            rotation = if (cz % 2 == 1) 0 else 1
                            setType(type)
                            setState(DoorState.DISCOVERED)
                        }
                        addDoor(newDoor)
                    } else {
                        door.setState(DoorState.DISCOVERED)
                        door.setType(type)
                    }
                }
            }
        }
    }

    fun clampMap(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double {
        return when {
            n <= inMin -> outMin
            n >= inMax -> outMax
            else -> (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin
        }
    }
}

//#if MC >= 1.21.5
@Stella.Command
object DsDebug : CommandUtils(
    "sadb"
) {
    override fun execute(context: CommandContext<FabricClientCommandSource>): Int {
        val room = DungeonScanner.currentRoom ?: return 0
        val name = room.name ?: "Unnamed"
        val cores = room.cores

        if (cores.isEmpty()) {
            ChatUtils.addMessage("${Stella.PREFIX} §b$name §fhas no scanned cores!")
        } else {
            ChatUtils.addMessage("${Stella.PREFIX} §b$name §fcore hash${if (cores.size > 1) "es" else ""}:")
            cores.forEach {
                ChatUtils.addMessage(" - $it")
            }

            ChatUtils.addMessage("${Stella.PREFIX} §b$name §ftype is §b${room.type}§f, with a checkmark of §b${room.checkmark}§f, explored? ${room.explored}")
        }
        return 1
    }
}
//#endif