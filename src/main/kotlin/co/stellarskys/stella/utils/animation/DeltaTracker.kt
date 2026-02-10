package co.stellarskys.stella.utils.animation

import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.TimeUtils.millis

class DeltaTracker(val targetFps: Float = 120f) {
    private var tracker = TimeUtils.now
    private val msPerFrame = 1000.0 / targetFps

    var currentDelta = 0.0
        private set

    /**
     * Calculates how many "target frames" have passed since the last call.
     * Use this in your Animation class.
     */
    fun getDelta(): Double {
        val elapsed = tracker.since.millis.toDouble()
        tracker = TimeUtils.now
        return elapsed / msPerFrame
    }

    fun updateDelta() {
        currentDelta = getDelta()
    }
}