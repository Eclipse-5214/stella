package co.stellarskys.stella.utils.skyblock.dungeons.utils

data class RoomMetadata(
    val name: String,
    val type: String,
    val cores: List<Int>,
    val secrets: Int = 0,
    val crypts: Int = 0,
    val trappedChests: Int = 0
)