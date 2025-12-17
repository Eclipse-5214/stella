package co.stellarskys.stella.events.api

class EventHandle<T : Event>(
    private val bus: EventBus,
    private val eventClass: Class<T>,
    private val handler: (T) -> Unit
) {
    private var registered = false

    fun register(): Boolean {
        if (registered) return false
        bus.add(eventClass, handler)
        registered = true
        return true
    }

    fun unregister(): Boolean {
        if (!registered) return false
        bus.remove(eventClass, handler)
        registered = false
        return true
    }

    fun isRegistered(): Boolean = registered
}
