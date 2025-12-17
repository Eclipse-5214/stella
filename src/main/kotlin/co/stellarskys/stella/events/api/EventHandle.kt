package co.stellarskys.stella.events.api

class EventHandle<T : Event>(
    private val bus: EventBus,
    val eventClass: Class<T>,
    private val handler: (T) -> Unit
) {
    private var registered = false
    private var wrapper: ((Event) -> Unit)? = null

    fun register(): Boolean {
        if (registered) return false
        wrapper = bus.add(eventClass, handler)
        registered = true
        return true
    }

    fun unregister(): Boolean {
        if (!registered) return false
        wrapper?.let { bus.remove(eventClass, it) }
        registered = false
        return true
    }
}
