package co.stellarskys.stella.features.secrets.utils

import co.stellarskys.stella.features.secrets.secretRoutes
import java.awt.Color

enum class WaypointType {
    START,
    SECRET,
    BAT,
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
            SECRET -> secretRoutes.secretColor.toColor()
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
            SECRET -> "Click"
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
    }
}