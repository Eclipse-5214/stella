package co.stellarskys.stella.api.dungeons.utils

data class RoomMetadata(
    val name: String,
    val type: String,
    val roomID: Int,
    val shape: String? = null,
    val cores: List<Int>,
    val secrets: Int = 0,
    val crypts: Int = 0,
    val trappedChests: Int = 0,
    val reviveStones: Int = 0
)
