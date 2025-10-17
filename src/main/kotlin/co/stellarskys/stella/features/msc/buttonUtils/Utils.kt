package co.stellarskys.stella.features.msc.buttonUtils

enum class AnchorType(val slots: Int) {
    // screen edges (5 each)
    SCREEN_TOP_LEFT(5),
    SCREEN_TOP_RIGHT(5),
    SCREEN_BOTTOM_LEFT(5),
    SCREEN_BOTTOM_RIGHT(5),
    SCREEN_TOP(5),
    SCREEN_BOTTOM(5),
    SCREEN_LEFT(5),
    SCREEN_RIGHT(5),

    // inventory sides (fill entire side)
    INVENTORY_TOP(7),
    INVENTORY_BOTTOM(7),
    INVENTORY_LEFT(6),
    INVENTORY_RIGHT(6),

    // player model corners (1 each)
    PLAYER_MODEL_TOP_LEFT(1),
    PLAYER_MODEL_TOP_RIGHT(1),
    PLAYER_MODEL_BOTTOM_LEFT(1),
    PLAYER_MODEL_BOTTOM_RIGHT(1)
}

data class StellaButton(
    var iconId: String,
    var command: String,
    val anchor: AnchorType,
    val index: Int = 0,
    val background: Boolean = true
)