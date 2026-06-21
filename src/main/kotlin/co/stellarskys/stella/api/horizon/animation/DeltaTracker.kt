package co.stellarskys.stella.api.horizon.animation

import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis

class DeltaTracker(targetFps: Float = 120f) {
    private var tracker = Chronos.now
    private val msPerFrame = 1000.0 / targetFps
    var frame: Long = 0
        private set

    var currentDelta = 0.0
        private set

    fun updateDelta() {
        currentDelta = tracker.since.millis.toDouble() / msPerFrame
        tracker = Chronos.now
        frame ++
    }
}