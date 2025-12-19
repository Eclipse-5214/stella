package co.stellarskys.stella.events.api

open class EventBus {
    @PublishedApi internal val subscribers = SubscriberSet()

    inline fun <reified T : Event> on(
        priority: Int = 0,
        register: Boolean = true,
        noinline handler: (T) -> Unit
    ): EventHandle<T> {
        val handle = EventHandle(T::class.java, handler, priority, this, register)
        subscribers.setup(T::class.java).add(handle)
        return handle
    }

    fun <T : Event> post(event: T): Boolean {
        @Suppress("UNCHECKED_CAST")
        subscribers.enabled[event.javaClass]?.forEach { (it as EventHandle<T>).handler(event) }
        return event.cancelable && event.cancelled
    }

    @PublishedApi internal fun updateEnabled(eventClass: Class<*>) {
        subscribers.enabled[eventClass] = subscribers.all[eventClass]?.filter { it.registered } ?: return
    }
}