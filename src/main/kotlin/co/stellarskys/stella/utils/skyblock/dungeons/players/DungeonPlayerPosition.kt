package co.stellarskys.stella.utils.skyblock.dungeons.players

import co.stellarskys.stella.utils.Utils

class DungeonPlayerPosition {
    val real = Real()
    class Real {
        var x: Double? = null
        var z: Double? = null
        var yaw: Double? = null
    }

    val icon = Icon()
    class Icon {
        var x: Double? = null
        var z: Double? = null
    }

    val lerped = Lerped(this)
    class Lerped(private val p: DungeonPlayerPosition) {
        val x: Double?
            get() = p.computeLerp()?.first
        val z: Double?
            get() = p.computeLerp()?.second
        val yaw: Double?
            get() = p.computeLerp()?.third
    }

    // interpolation state
    private var lastX: Double? = null
    private var lastZ: Double? = null
    private var lastYaw: Double? = null
    private var lastTime: Double? = null

    private var currX: Double? = null
    private var currZ: Double? = null
    private var currYaw: Double? = null
    private var currTime: Double? = null

    // update function
    fun updatePosition(realX: Double, realZ: Double, yaw: Float, iconX: Double?, iconZ: Double?) {
        val now = System.nanoTime() * 1e-6

        lastX = currX
        lastZ = currZ
        lastYaw = currYaw
        lastTime = currTime

        currX = realX
        currZ = realZ
        currYaw = yaw.toDouble()
        currTime = now

        real.x = realX
        real.z = realZ
        real.yaw = yaw.toDouble()

        if (iconX != null) icon.x = iconX
        if (iconZ != null) icon.z = iconZ
    }

    private fun computeLerp(): Triple<Double, Double, Double>? {
        val lx = lastX ?: return currX?.let { Triple(it, currZ!!, currYaw!!) }
        val lz = lastZ ?: return currX?.let { Triple(it, currZ!!, currYaw!!) }
        val ly = lastYaw ?: return currX?.let { Triple(it, currZ!!, currYaw!!) }

        val cx = currX ?: return null
        val cz = currZ ?: return null
        val cy = currYaw ?: return null

        val lt = lastTime ?: return Triple(cx, cz, cy)
        val ct = currTime ?: return Triple(cx, cz, cy)

        val now = System.nanoTime() * 1e-6
        val f = (now - ct) / (ct - lt)

        return Triple(
            Utils.lerp(f, lx, cx),
            Utils.lerp(f, lz, cz),
            Utils.lerpAngle(f, ly, cy)
        )
    }
}