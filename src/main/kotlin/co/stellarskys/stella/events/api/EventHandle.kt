package co.stellarskys.stella.events.api

class EventHandle<T : Event>(
    val eventClass: Class<T>,
    val handler: (T) -> Unit,
    val priority: Int,
    val ignoreCancelled: Boolean,
    val bus: EventBus,
    @Volatile private var _registered: Boolean
) {
    val registered: Boolean get() = _registered

    fun register(): Boolean = toggle(bus, true)
    fun unregister(): Boolean = toggle(bus, false)

    private fun toggle(bus: EventBus, value: Boolean): Boolean {
        if (_registered == value) return false
        _registered = value
        bus.rebuildCache(eventClass)
        return true
    }
}