package co.stellarskys.stella.utils.skyblock.dungeons.utils

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