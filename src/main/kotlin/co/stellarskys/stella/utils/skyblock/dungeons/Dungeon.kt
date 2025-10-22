package co.stellarskys.stella.utils.skyblock.dungeons

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.*
import co.stellarskys.stella.utils.*
import co.stellarskys.stella.utils.skyblock.LocationUtils
import co.stellarskys.stella.utils.skyblock.dungeons.map.*
import co.stellarskys.stella.utils.skyblock.dungeons.players.DungeonPlayerManager
import co.stellarskys.stella.utils.skyblock.dungeons.score.*
import co.stellarskys.stella.utils.skyblock.dungeons.utils.*

/**
 * Central dungeon state manager.
 * Basically one-stop shop for everything dungeons
 */
object Dungeon {

    // 🧩 Regex patterns for chat parsing
    private val WATCHER_PATTERN = Regex("""\[BOSS] The Watcher: That will be enough for now\.""")
    private val DUNGEON_COMPLETE_PATTERN = Regex("""^\s*(Master Mode)?\s?(?:The)? Catacombs - (Entrance|Floor .{1,3})$""")
    private val ROOM_SECRETS_PATTERN = Regex("""\b([0-9]|10)/([0-9]|10)\s+Secrets\b""")

    // Room and door data
    val rooms = Array<Room?>(36) { null }
    val doors = Array<Door?>(60) { null }
    val uniqueRooms = mutableSetOf<Room>()
    val uniqueDoors = mutableSetOf<Door>()
    val discoveredRooms = mutableMapOf<String, DiscoveredRoom>()

    // Dungeon state
    var bloodClear = false
    var bloodDone = false
    var complete = false
    var currentRoom: Room? = null
    var holdingLeaps = false

    // Floor info
    val floor: String? get() = LocationUtils.dungeonFloor
    val floorNumber: Int? get() = LocationUtils.dungeonFloorNum
    var inDungeon = false
    val inBoss: Boolean
        get() = floor != null && Stella.mc.player?.let {
            val (x, z) = WorldScanUtils.realCoordToComponent(it.x.toInt(), it.z.toInt())
            6 * z + x > 35
        } == true

    // HUD lines
    var mapLine1 = ""
    var mapLine2 = ""

    data class DiscoveredRoom(val x: Int, val z: Int, val room: Room)

    /** Initializes all dungeon systems and event listeners */
    fun init() {
        EventBus.register<AreaEvent.Main> {
            TickUtils.scheduleServer(1) {
                inDungeon = LocationUtils.area == "catacombs"
                if (!inDungeon) reset()
                WorldScanner.updater.register()
            }
        }

        EventBus.register<WorldEvent.Change> { reset() }

        EventBus.register<ChatEvent.Receive> { event ->
            if (!inDungeon) return@register
            val msg = event.message.string.clearCodes()
            if (DUNGEON_COMPLETE_PATTERN.containsMatchIn(msg)) complete = true
            if (WATCHER_PATTERN.containsMatchIn(msg)) bloodDone = true
        }

        EventBus.register<GameEvent.ActionBar> { event ->
            if (!inDungeon) return@register
            val room = currentRoom ?: return@register
            val match = ROOM_SECRETS_PATTERN.find(event.message.string.clearCodes()) ?: return@register
            val (found, _) = match.destructured
            val secrets = found.toInt()
            if (secrets != room.secretsFound) room.secretsFound = secrets
        }

        EventBus.register<TickEvent.Client> {
            if (!inDungeon) return@register
            updateHudLines()
            updateHeldItem()
        }

        RoomRegistry.loadFromRemote()
        DungeonPlayerManager.init()
        DungeonScore.init()
        MapUtils.init()
    }

    /** Clears all dungeon state */
    fun reset() {
        rooms.fill(null)
        doors.fill(null)
        uniqueRooms.clear()
        uniqueDoors.clear()
        discoveredRooms.clear()
        currentRoom = null
        bloodClear = false
        bloodDone = false
        holdingLeaps = false
        mapLine1 = ""
        mapLine2 = ""
        WorldScanner.reset()
        DungeonPlayerManager.reset()
        DungeonScore.reset()
        MapUtils.reset()
    }

    /** Updates HUD lines for map overlay */
    private fun updateHudLines() {
        val run = DungeonScore.data

        val dSecrets = "§7Secrets: §b${run.secretsFound}§8-§e${run.secretsRemaining}§8-§c${run.totalSecrets}"
        val dCrypts = "§7Crypts: " + when {
            run.crypts >= 5 -> "§a${run.crypts}"
            run.crypts > 0  -> "§e${run.crypts}"
            else            -> "§c0"
        }
        val dMimic = if (floorNumber in listOf(6, 7)) {
            "§7Mimic: " + if (MimicTrigger.mimicDead) "§a✔" else "§c✘"
        } else ""

        val minSecrets = "§7Min Secrets: " + when {
            run.secretsFound == 0 -> "§b?"
            run.minSecrets > run.secretsFound -> "§e${run.minSecrets}"
            else -> "§a${run.minSecrets}"
        }

        val dDeaths = "§7Deaths: " + if (run.teamDeaths < 0) "§c${run.teamDeaths}" else "§a0"
        val dScore = "§7Score: " + when {
            run.score >= 300 -> "§a${run.score}"
            run.score >= 270 -> "§e${run.score}"
            else             -> "§c${run.score}"
        } + if (DungeonScore.hasPaul) " §b★" else ""

        mapLine1 = "$dSecrets    $dCrypts    $dMimic".trim()
        mapLine2 = "$minSecrets    $dDeaths    $dScore".trim()
    }

    /** Updates leap detection based on held item */
    private fun updateHeldItem() {
        val item = Stella.mc.player?.mainHandStack ?: return
        holdingLeaps = "leap" in item.name.string.clearCodes().lowercase()
    }

    // Room accessors
    fun getRoomIdx(comp: Pair<Int, Int>) = 6 * comp.second + comp.first
    fun getRoomAtIdx(idx: Int) = rooms.getOrNull(idx)
    fun getRoomAtComp(comp: Pair<Int, Int>) = getRoomAtIdx(getRoomIdx(comp))
    fun getRoomAt(x: Int, z: Int) = getRoomAtComp(WorldScanUtils.realCoordToComponent(x, z))

    // Door accessors
    fun getDoorIdx(comp: Pair<Int, Int>): Int {
        val base = ((comp.first - 1) shr 1) + 6 * comp.second
        return base - (base / 12)
    }

    fun getDoorAtIdx(idx: Int) = doors.getOrNull(idx)
    fun getDoorAtComp(comp: Pair<Int, Int>) = getDoorAtIdx(getDoorIdx(comp))
    fun getDoorAt(x: Int, z: Int) = getDoorAtComp(WorldScanUtils.realCoordToComponent(x, z))

    /** Adds a door to the map and tracks it as unique */
    fun addDoor(door: Door) {
        val idx = getDoorIdx(door.getComp())
        if (idx in doors.indices) {
            doors[idx] = door
            uniqueDoors += door
        }
    }

    /** Merges two rooms into one unified instance */
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

    /** Removes a room from the map grid */
    fun removeRoom(room: Room) {
        for (comp in room.components) {
            val idx = getRoomIdx(comp)
            if (idx in rooms.indices) rooms[idx] = null
        }
    }
}