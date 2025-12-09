package co.stellarskys.stella.events.api

abstract class CancellableEvent : Event() {
    var cancelled = false
        private set

    fun cancel() {
        cancelled = true
    }
}