package co.stellarskys.stella.utils.skyblock.dungeons.players

import co.stellarskys.stella.utils.Utils

class DungeonPlayerPosition {
    val raw = PosData()

    private var last = PosData()
    private var curr = PosData()
    private var lastTime: Double? = null
    private var currTime: Double? = null

    fun updatePosition(realX: Double, realZ: Double, yaw: Float, iconX: Double, iconZ: Double) {
        val now = System.nanoTime() * 1e-6

        last = curr.copy()
        lastTime = currTime

        curr.realX = realX
        curr.realZ = realZ
        curr.yaw = yaw.toDouble()

        curr.iconX = iconX
        curr.iconZ = iconZ
        currTime = now

        raw.realX = realX
        raw.realZ = realZ
        raw.yaw = yaw.toDouble()

        raw.iconX = iconX
        raw.iconZ = iconZ
    }

    fun getLerped(): PosData? {
        val lt = lastTime ?: return null
        val ct = currTime ?: return null

        val now = System.nanoTime() * 1e-6
        val f = ((now - ct) / (ct - lt)).coerceIn(0.0, 1.0)

        return PosData(
            realX = Utils.lerp(f, last.realX!!, curr.realX!!),
            realZ = Utils.lerp(f, last.realZ!!, curr.realZ!!),
            iconX = Utils.lerp(f, last.iconX!!, curr.iconX!!),
            iconZ = Utils.lerp(f, last.iconZ!!, curr.iconZ!!),
            yaw   = Utils.lerpAngle(f, last.yaw!!, curr.yaw!!)
        )
    }

    data class PosData(
        var realX: Double? = null,
        var realZ: Double? = null,
        var iconX: Double? = null,
        var iconZ: Double? = null,
        var yaw: Double? = null
    )
}
