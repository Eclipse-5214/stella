package co.stellarskys.stella.events.api

open class EventBus {
    @PublishedApi internal val subscribers = SubscriberSet()

    inline fun <reified T : Event> on(
        priority: Int = 0,
        register: Boolean = true,
        noinline handler: (T) -> Unit
    ) = EventHandle(T::class.java, handler, priority, this, register).also {
        subscribers.setup(T::class.java).add(it)
    }

    fun <T : Event> post(event: T): Boolean {
        @Suppress("UNCHECKED_CAST")
        val handles = subscribers.enabled[event.javaClass] as? Array<EventHandle<T>> ?: return false
        for (h in handles) h.handler(event)
        return event.cancelable && event.cancelled
    }

    @PublishedApi internal fun updateEnabled(eventClass: Class<*>) {
        subscribers.enabled[eventClass] = subscribers.all[eventClass]?.filter { it.registered }?.toTypedArray() ?: return
    }
}