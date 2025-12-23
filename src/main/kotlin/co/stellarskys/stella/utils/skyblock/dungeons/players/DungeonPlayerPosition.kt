package co.stellarskys.stella.utils.skyblock.dungeons.players

import co.stellarskys.stella.utils.Utils

class DungeonPlayerPosition {
    val raw = Raw()
    class Raw {
        var IconX: Double? = null
        var IconZ: Double? = null
        var RealX: Double? = null
        var RealZ: Double? = null
        var yaw: Double? = null
    }


    // LERPED VIEW
    val lerped = Lerped(this)
    class Lerped(private val p: DungeonPlayerPosition) {
        private val data get() = p.computeLerp()

        val IconX get() = data?.iconX
        val IconZ get() = data?.iconZ
        val RealX get() = data?.realX
        val RealZ get() = data?.realZ
        var yaw: Double? = data?.yaw

        val real = Real(p)
        class Real(private val p: DungeonPlayerPosition) {
            val x get() = p.computeLerp()?.realX
            val z get() = p.computeLerp()?.realZ
            val yaw get() = p.computeLerp()?.realYaw
        }

        val icon = Icon(p)
        class Icon(private val p: DungeonPlayerPosition) {
            val x get() = p.computeLerp()?.iconX
            val z get() = p.computeLerp()?.iconZ
            val yaw get() = p.computeLerp()?.iconYaw
        }
    }

    private var lastRealX: Double? = null
    private var lastRealZ: Double? = null
    private var lastRealYaw: Double? = null

    private var lastIconX: Double? = null
    private var lastIconZ: Double? = null
    private var lastIconYaw: Double? = null

    private var lastTime: Double? = null

    private var currRealX: Double? = null
    private var currRealZ: Double? = null
    private var currYaw: Double? = null

    private var currIconX: Double? = null
    private var currIconZ: Double? = null
    private var currIconYaw: Double? = null

    private var currTime: Double? = null

    fun updatePosition(realX: Double, realZ: Double, yaw: Float, iconX: Double, iconZ: Double) {
        val now = System.nanoTime() * 1e-6

        // shift current → last
        lastRealX = currRealX
        lastRealZ = currRealZ
        lastRealYaw = currRealYaw

        lastIconX = currIconX
        lastIconZ = currIconZ
        lastIconYaw = currIconYaw

        lastTime = currTime

        // store new
        currRealX = realX
        currRealZ = realZ
        currRealYaw = yaw.toDouble()

        currIconX = iconX
        currIconZ = iconZ
        currIconYaw = yaw.toDouble()

        currTime = now

        // update raw
        raw.RealX = realX
        raw.RealZ = realZ

        raw.IconX = iconX
        raw.IconZ = iconZ
    }

    private fun computeLerp(): LerpResult? {
        val lt = lastTime ?: return null
        val ct = currTime ?: return null

        val now = System.nanoTime() * 1e-6
        val f = ((now - ct) / (ct - lt)).coerceIn(0.0, 1.0)

        return LerpResult(
            realX = Utils.lerp(f, lastRealX ?: currRealX!!, currRealX!!),
            realZ = Utils.lerp(f, lastRealZ ?: currRealZ!!, currRealZ!!),
            yaw = Utils.lerpAngle(f, lastRealYaw ?: currRealYaw!!, currRealYaw!!),

            iconX = Utils.lerp(f, lastIconX ?: currIconX!!, currIconX!!),
            iconZ = Utils.lerp(f, lastIconZ ?: currIconZ!!, currIconZ!!),
        )
    }

    data class LerpResult(
        val realX: Double,
        val realZ: Double,
        val yaw: Double,
        val iconX: Double,
        val iconZ: Double,
    )
}
