package co.stellarskys.stella.features.secrets.utils

import co.stellarskys.stella.features.secrets.secretRoutes
import java.awt.Color

enum class WaypointType {
    START,
    BAT,
    CHEST,
    ESSENCE,
    ITEM,
    MINE,
    LEVER,
    SUPERBOOM,
    ETHERWARP,
    PEARL,
    CUSTOM,
    ;

    val color: Color
        get() = when (this) {
            BAT -> secretRoutes.batColor
            MINE -> secretRoutes.mineColor
            CHEST -> secretRoutes.chestColor
            ITEM -> secretRoutes.itemColor
            ESSENCE -> secretRoutes.essenceColor
            ETHERWARP -> secretRoutes.etherWarpColor
            PEARL -> secretRoutes.pearlColor
            SUPERBOOM -> secretRoutes.superBoomColor
            LEVER -> secretRoutes.leverColor
            START -> secretRoutes.startColor
            CUSTOM -> Color.WHITE
        }


    val label: String
        get() = when (this) {
            BAT -> "Bat"
            MINE -> "Mine"
            CHEST, ESSENCE -> "Click"
            ITEM -> "Item"
            ETHERWARP -> "Warp"
            PEARL -> "Pearl"
            SUPERBOOM -> "Boom!"
            LEVER -> "Flick"
            START -> ""
            CUSTOM -> name
        }

    companion object {
        fun fromString(value: String?): WaypointType? {
            if (value == null) return null
            return entries.find { it.name.equals(value, ignoreCase = true) }
        }

        val SECRET = setOf(CHEST, ITEM, ESSENCE, BAT)
    }
}