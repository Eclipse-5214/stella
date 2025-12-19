package co.stellarskys.stella.events.api

class EventHandle<T : Event>(
    val eventClass: Class<T>,
    val handler: (T) -> Unit,
    val priority: Int,
    val bus: EventBus,
) {
    var registered = false
        private set

    fun register(): Boolean {
        if (registered) return false
        registered = true
        bus.updateEnabled(eventClass)
        return true
    }

    fun unregister(): Boolean {
        if (!registered) return false
        registered = false
        bus.updateEnabled(eventClass)
        return true
    }
}