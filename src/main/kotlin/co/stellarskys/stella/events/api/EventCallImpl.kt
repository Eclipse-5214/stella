package co.stellarskys.stella.events.api

internal class EventCallImpl<T : Event>(
    private val callback: PrioritizedCallback<T>,
    private val handlers: MutableList<PrioritizedCallback<*>>,
    private val eventBus: EventBus,
    initiallyRegistered: Boolean
) : EventCall {

    private var registered = initiallyRegistered

    override fun unregister(): Boolean {
        if (!registered) return false
        val removed = handlers.remove(callback)
        if (removed) {
            registered = false
        }
        return removed
    }

    override fun register(): Boolean {
        if (registered) return false
        eventBus.addSorted(handlers, callback)
        registered = true
        return true
    }

    override fun isRegistered(): Boolean = registered
}