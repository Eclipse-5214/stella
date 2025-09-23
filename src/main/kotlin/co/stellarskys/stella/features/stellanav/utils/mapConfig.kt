package co.stellarskys.stella.features.stellanav.utils

import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.config.RGBA
import java.awt.Color

object mapConfig {
    val bossMapEnabled: Boolean get() = config["bossMapEnabled"] as? Boolean ?: false
    val scoreMapEnabled: Boolean get() = config["scoreMapEnabled"] as? Boolean ?: false

    val checkmarkScale: Float get() = config["checkmarkScale"] as? Float ?: 1f
    val roomCheckmarks: Int get() = config["roomCheckmarks"] as? Int ?: 0
    val rcsize: Float get() = config["rcsize"] as? Float ?: 1f
    val puzzleCheckmarks: Int get() = config["puzzleCheckmarks"] as? Int ?: 0
    val pcsize: Float get() = config["pcsize"] as? Float ?: 1f

    val mapInfoUnder: Boolean get() = config["mapInfoUnder"] as? Boolean ?: false

    val noKey: Color get() = (config["noKeyColor"] as? RGBA)?.toColor() ?: Color.red
    val key: Color get() = (config["keyColor"] as? RGBA)?.toColor() ?: Color.green
    val doorLW: Double get() = (config["doorLineWidth"] as? Int ?: 3).toDouble()

    // map colors
    val NormalColor: Color get() = (config["normalRoomColor"] as? RGBA)?.toColor() ?: Color(107, 58, 17, 255)
    val PuzzleColor: Color get() = (config["puzzleRoomColor"] as? RGBA)?.toColor() ?: Color(117, 0, 133, 255)
    val TrapColor: Color get() = (config["trapRoomColor"] as? RGBA)?.toColor() ?: Color(216, 127, 51, 255)
    val MinibossColor: Color get() = (config["minibossRoomColor"] as? RGBA)?.toColor() ?: Color(254, 223, 0, 255)
    val BloodColor: Color get() = (config["bloodRoomColor"] as? RGBA)?.toColor() ?: Color(255, 0, 0, 255)
    val FairyColor: Color get() = (config["fairyRoomColor"] as? RGBA)?.toColor() ?: Color(224, 0, 255, 255)
    val EntranceColor: Color get() = (config["entranceRoomColor"] as? RGBA)?.toColor() ?: Color(20, 133, 0, 255)

    val NormalDoorColor: Color get() = (config["normalDoorColor"] as? RGBA)?.toColor() ?: Color(80, 40, 10, 255)
    val WitherDoorColor: Color get() = (config["witherDoorColor"] as? RGBA)?.toColor() ?: Color(0, 0, 0, 255)
    val BloodDoorColor: Color get() = (config["bloodDoorColor"] as? RGBA)?.toColor() ?: Color(255, 0, 0, 255)
    val EntranceDoorColor: Color get() = (config["entranceDoorColor"] as? RGBA)?.toColor() ?: Color(0, 204, 0, 255)

    // class Colors
    val healerColor: Color get() = (config["healerColor"] as? RGBA)?.toColor() ?: Color(240, 70, 240, 255)
    val mageColor: Color get() = (config["mageColor"] as? RGBA)?.toColor() ?: Color(70, 210, 210, 255)
    val berzColor: Color get() = (config["berzColor"] as? RGBA)?.toColor() ?: Color(255, 0, 0, 255)
    val archerColor: Color get() = (config["archerColor"] as? RGBA)?.toColor() ?: Color(254, 223, 0, 255)
    val tankColor: Color get() = (config["tankColor"] as? RGBA)?.toColor() ?: Color(30, 170, 50, 255)

    // icon settings
    val iconScale: Float get() = config["iconScale"] as? Float ?: 1f
    val showPlayerHead: Boolean get() = config["showPlayerHeads"] as? Boolean ?: false
    val iconBorderWidth: Float get() = config["iconBorderWidth"] as? Float ?: 0.2f
    val iconBorderColor: Color get() = (config["iconBorderColor"] as? RGBA)?.toColor() ?: Color(0, 0, 0, 255)
    val iconClassColors: Boolean get() = config["iconClassColors"] as? Boolean ?: false
    val showNames: Boolean get() = config["showNames"] as? Boolean ?: false
    val dontShowOwn: Boolean get() = config["dontShowOwn"] as? Boolean ?: false

    // other colors
    val mapBgColor: Color get() = (config["mapBgColor"] as? RGBA)?.toColor() ?: Color(0, 0, 0, 100)
    val mapBdColor: Color get() = (config["mapBdColor"] as? RGBA)?.toColor() ?: Color(0, 0, 0, 255)
    val mapBorder: Boolean get() = config["mapBorder"] as? Boolean ?: true
    val mapBdWidth: Int get() = config["mapBdWidth"] as? Int ?: 2
}