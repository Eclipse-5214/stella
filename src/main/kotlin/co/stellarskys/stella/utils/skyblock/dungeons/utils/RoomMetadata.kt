package co.stellarskys.stella.utils.skyblock.dungeons.utils

data class RoomMetadata(
    val name: String,
    val type: String,
    val shape: String? = null,
    val cores: List<Int>,
    val secretDetails: SecretDetails? = null,
    val secretCoords: SecretCoords? = null,
    val secrets: Int = 0,
    val crypts: Int = 0,
    val trappedChests: Int = 0,
    val reviveStones: Int = 0
) {
    data class SecretDetails(
        val redstoneKey: Int = 0,
        val wither: Int = 0,
        val bat: Int = 0,
        val item: Int = 0,
        val chest: Int = 0
    )

    data class SecretCoords(
        val redstoneKey: List<Coord> = emptyList(),
        val wither: List<Coord> = emptyList(),
        val bat: List<Coord> = emptyList(),
        val item: List<Coord> = emptyList(),
        val chest: List<Coord> = emptyList()
    ) {
        fun allWithTypes(): List<Pair<String, Coord>> {
            return buildList {
                redstoneKey.forEach { add("Redstone Key" to it) }
                wither.forEach      { add("Wither"       to it) }
                bat.forEach         { add("Bat"          to it) }
                item.forEach        { add("Item"         to it) }
                chest.forEach       { add("Chest"        to it) }
            }
        }
    }

    data class Coord(
        val x: Int,
        val y: Int,
        val z: Int
    )
}
