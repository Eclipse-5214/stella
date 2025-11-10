package co.stellarskys.stella.utils.skyblock.dungeons.utils

/**
 * @author SkyblockAPI
 */
enum class DungeonFloor(
    val bossName: String,
    val chatBossName: String = bossName,
    val floorNumber: Int,
) {
    E("The Watcher", 0),

    F1("Bonzo", 1),
    F2("Scarf", 2),
    F3("The Professor", 3),
    F4("Thorn", 4),
    F5("Livid", 5),
    F6("Sadan", 6),
    F7("Necron", "Maxor", 7),

    M1("Bonzo", 1),
    M2("Scarf", 2),
    M3("The Professor", 3),
    M4("Thorn", 4),
    M5("Livid", 5),
    M6("Sadan", 6),
    M7("Necron", "Maxor", 7),
    ;

    constructor(bossName: String, floorNumber: Int) : this(bossName, bossName, floorNumber)

    companion object {
        fun getByName(name: String) = runCatching { DungeonFloor.valueOf(name) }.getOrNull()
    }
}

enum class DungeonClass (val displayName: String) {
    UNKNOWN("Unknown"),
    HEALER("Healer"),
    MAGE("Mage"),
    BERSERK("Berserk"),
    ARCHER("Archer"),
    TANK("Tank"),
    DEAD("DEAD");

    companion object {
        private val classes: Map<String, DungeonClass> = entries.toTypedArray().associateBy { it.displayName }

        fun from(name: String): DungeonClass = classes[name] ?: UNKNOWN
    }
}

enum class DoorType { NORMAL, WITHER, BLOOD, ENTRANCE }
enum class DoorState { UNDISCOVERED, DISCOVERED }
enum class Checkmark { NONE, WHITE, GREEN, FAILED, UNEXPLORED, UNDISCOVERED }
enum class RoomType { NORMAL, PUZZLE, TRAP, YELLOW, BLOOD, FAIRY, RARE, ENTRANCE, UNKNOWN; }