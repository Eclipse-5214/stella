package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.AreaEvent
import co.stellarskys.stella.events.ChatEvent
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.GameEvent
import co.stellarskys.stella.events.TickEvent
import co.stellarskys.stella.events.WorldEvent
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.clearCodes
import co.stellarskys.stella.utils.skyblock.LocationUtils
import co.stellarskys.stella.utils.skyblock.dungeons.map.Door
import co.stellarskys.stella.utils.skyblock.dungeons.map.Room
import co.stellarskys.stella.utils.skyblock.dungeons.map.WorldScanner
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.dungeons.score.DungeonScore
import co.stellarskys.stella.utils.skyblock.dungeons.score.MimicTrigger
import co.stellarskys.stella.utils.skyblock.dungeons.score.ScoreData
import co.stellarskys.stella.utils.skyblock.dungeons.utils.MapUtils
import co.stellarskys.stella.utils.skyblock.dungeons.utils.RoomRegistry
import co.stellarskys.stella.utils.skyblock.dungeons.utils.WorldScanUtils

object Dungeon {
    // Regex
    val WATCHER_PATTERN = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    val DUNGEON_COMPLETE_PATTERN = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")
    val ROOM_SECRETS_PATTERN = Regex("""\b([0-9]|10)/([0-9]|10)\s+Secrets\b""")

    // Clear Map
    val rooms = Array<Room?>(36) { null }
    val doors = Array<Door?>(60) { null }
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>()
    val discoveredRooms = mutableMapOf<String, DiscoveredRoom>()
    var bloodClear = false
    var bloodDone = false
    var complete = false

    // Player
    var currentRoom: Room? = null
    var holdingLeaps: Boolean = false

    // floor
    val floor: String? get() = LocationUtils.dungeonFloor
    val floorNumber: Int? get() = LocationUtils.dungeonFloorNum
    var inDungeon = false
    val inBoss get() = floor != null &&
            Stella.mc.player?.let {
                val (x, z) = WorldScanUtils.realCoordToComponent(it.x.toInt(), it.z.toInt())
                6 * z + x > 35
            } == true

    // else
    var mapLine1 = ""
    var mapLine2 = ""

    data class DiscoveredRoom(
        val x: Int,
        val z: Int,
        val room: Room,
    )

    fun init() {
        EventBus.register<AreaEvent.Main> ({
            TickUtils.scheduleServer(1) {
                if (LocationUtils.area != "catacombs") {
                    reset()
                }

                inDungeon = LocationUtils.area == "catacombs"
                WorldScanner.updater.register()
            }
        })

        EventBus.register<WorldEvent.Change> { reset() }

        EventBus.register<ChatEvent.Receive>({ event ->
            if (!inDungeon) return@register
            val msg = event.message.string.clearCodes()

            val compleateMatch = DUNGEON_COMPLETE_PATTERN.find(msg)

            if (compleateMatch != null) {
                complete = true
                return@register
            }

            val watcherMatch = WATCHER_PATTERN.find(msg)

            if (watcherMatch != null) {
                bloodClear = true
                return@register
            }
        })

        EventBus.register<GameEvent.ActionBar> { event ->
            if (!inDungeon) return@register

            val room = currentRoom ?: return@register
            val match = ROOM_SECRETS_PATTERN.find(event.message.string.clearCodes()) ?: return@register
            val (found, total) = match.destructured
            val secrets = found.toInt()

            if (secrets == room.secretsFound) return@register
            room.secretsFound = secrets
        }

        EventBus.register<TickEvent.Client> {
            if (!inDungeon) return@register

            val dSecrets = "§7Secrets: " + "§b${DungeonScore.secretsFound}§8-§e${DungeonScore.scoreData.secretsRemaining}§8-§c${DungeonScore.scoreData.totalSecrets}"
            val dCrypts = "§7Crypts: " + when {DungeonScore.crypts >= 5 -> "§a${DungeonScore.crypts}"; DungeonScore.crypts > 0 -> "§e${DungeonScore.crypts}"; else -> "§c0" }
            val dMimic = if (floorNumber in listOf(6, 7)) { "§7Mimic: " + if (MimicTrigger.mimicDead) "§a✔" else "§c✘" } else { "" }
            val minSecrets = "§7Min Secrets: " + if (DungeonScore.secretsFound == 0) { "§b?" } else if (DungeonScore.scoreData.minSecrets > DungeonScore.secretsFound) { "§e${DungeonScore.scoreData.minSecrets}" } else { "§a${DungeonScore.scoreData.minSecrets}" }
            val dDeaths = "§7Deaths: " + if (DungeonScore.teamDeaths < 0) { "§c${DungeonScore.teamDeaths}" } else { "§a0" }
            val dScore = "§7Score: " + when {DungeonScore.scoreData.score >= 300 -> "§a${DungeonScore.scoreData.score}"; DungeonScore.scoreData.score >= 270 -> "§e${DungeonScore.scoreData.score}"; else -> "§c${DungeonScore.scoreData.score}" } + if (DungeonScore.hasPaul) " §b★" else ""

            mapLine1 = "$dSecrets    $dCrypts    $dMimic".trim()
            mapLine2 = "$minSecrets    $dDeaths    $dScore".trim()

            val player = Stella.mc.player ?: return@register
            val heldItem = player.mainHandStack ?: return@register
            val displayName = heldItem.name.string.clearCodes()

            holdingLeaps = "leap" in displayName.lowercase()
        }

        RoomRegistry.loadFromRemote()
        DungeonPlayerManager.init()
        DungeonScore.init()
        MapUtils.init()
    }

    fun reset() {
        rooms.fill(null)
        doors.fill(null)
        uniqueRooms.clear()
        uniqueDoors.clear()
        discoveredRooms.clear()
        currentRoom = null
        bloodDone = false
        bloodClear = false
        holdingLeaps = false
        mapLine1 = ""
        mapLine2 = ""
        WorldScanner.reset()
        DungeonPlayerManager.reset()
        DungeonScore.reset()
        MapUtils.reset()
    }

    fun getRoomAtIdx(idx: Int): Room? {
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomAtComp(comp: Pair<Int, Int>): Room? {
        val idx = getRoomIdx(comp)
        return if (idx in rooms.indices) rooms[idx] else null
    }

    fun getRoomAt(x: Int, z: Int): Room? {
        val comp = WorldScanUtils.realCoordToComponent(x, z)
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
        val comp = WorldScanUtils.realCoordToComponent(x, z)
        return getDoorAtComp(comp)
    }

    fun addDoor(door: Door) {
        val idx = getDoorIdx(door.getComp())
        if (idx !in doors.indices) return

        doors[idx] = door
        uniqueDoors += door
    }

    fun mergeRooms(room1: Room, room2: Room) {
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
}