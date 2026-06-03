package co.stellarskys.stella.api.dungeons.players

import co.stellarskys.stella.api.horizon.animation.Interpolator
import co.stellarskys.stella.utils.Utils

class DungeonPlayerPosition {
    private val interpolator = PosData.Engine

    var raw = PosData()
        private set

    fun updatePosition(realX: Double, realZ: Double, yaw: Float, iconX: Double, iconZ: Double) {
        raw = PosData(realX, realZ, iconX, iconZ, yaw.toDouble())
        interpolator.update(raw)
    }

    fun getLerped(): PosData = interpolator.get() ?: raw

    data class PosData(
        val realX: Double = 0.0,
        val realZ: Double = 0.0,
        val iconX: Double = 0.0,
        val iconZ: Double = 0.0,
        val yaw: Double = 0.0
    ) {
        companion object {
            val Engine = Interpolator<PosData> { f, s, e ->
                PosData(
                    realX = Utils.lerp(f, s.realX, e.realX),
                    realZ = Utils.lerp(f, s.realZ, e.realZ),
                    iconX = Utils.lerp(f, s.iconX, e.iconX),
                    iconZ = Utils.lerp(f, s.iconZ, e.iconZ),
                    yaw   = Utils.lerpAngle(f, s.yaw, e.yaw)
                )
            }
        }
    }
}