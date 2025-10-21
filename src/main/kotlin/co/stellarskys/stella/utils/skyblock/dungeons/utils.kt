package co.stellarskys.stella.utils.skyblock.dungeons

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