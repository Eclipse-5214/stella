package co.stellarskys.stella.events.api

open class EventBus {
    @PublishedApi internal val subscribers = SubscriberSet()

    inline fun <reified T : Event> on(
        priority: Int = 0,
        register: Boolean = true,
        noinline handler: (T) -> Unit
    ): EventHandle<T> {
        val handle = EventHandle(T::class.java, handler, priority, this)
        subscribers.setup(T::class.java).add(handle)
        if (register) handle.register()
        return handle
    }

    fun post(event: Event): Boolean {
        subscribers.enabled[event::class.java]?.forEach { handle ->
            val typed = handle as EventHandle<Event>
            if (event.cancelled) return@forEach
            try { typed.handler(event) } catch (_: Exception) {}
        }
        return event.cancelable && event.cancelled
    }

    @PublishedApi internal fun updateEnabled(eventClass: Class<*>) {
        val list = subscribers.all[eventClass] ?: return
        subscribers.enabled[eventClass] = list.filter { it.registered }
    }
}