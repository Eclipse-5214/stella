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
    CUSTOM,
    ;

    val color: Color
        get() = when (this) {
            BAT -> secretRoutes.batColor.toColor()
            MINE -> secretRoutes.mineColor.toColor()
            CHEST -> secretRoutes.chestColor.toColor()
            ITEM -> secretRoutes.itemColor.toColor()
            ESSENCE -> secretRoutes.essenceColor.toColor()
            ETHERWARP -> secretRoutes.etherWarpColor.toColor()
            SUPERBOOM -> secretRoutes.superBoomColor.toColor()
            LEVER -> secretRoutes.leverColor.toColor()
            START -> secretRoutes.startColor.toColor()
            CUSTOM -> Color.WHITE
        }


    val label: String
        get() = when (this) {
            BAT -> "Bat"
            MINE -> "Mine"
            CHEST, ESSENCE -> "Click"
            ITEM -> "Item"
            ETHERWARP -> "Warp"
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