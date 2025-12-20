package co.stellarskys.stella.events.api

class EventHandle<T : Event>(
    val eventClass: Class<T>,
    val handler: (T) -> Unit,
    val priority: Int,
    val bus: EventBus
) {
    @Volatile var registered = false
        private set

    fun setRegistered(value: Boolean): Boolean {
        if (registered == value) return false
        registered = value
        bus.updateEnabled(eventClass)
        return true
    }

    fun register() = setRegistered(true)
    fun unregister() = setRegistered(false)
}