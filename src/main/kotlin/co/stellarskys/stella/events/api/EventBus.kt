package co.stellarskys.stella.events.api

import co.stellarskys.stella.utils.ChatUtils

open class EventBus {
    private val subscribers = mutableMapOf<Class<*>, MutableList<(Event) -> Unit>>()

    inline fun <reified T : Event> on(
        register: Boolean = true,
        noinline handler: (T) -> Unit
    ): EventHandle<T> {
        val handle = EventHandle(this, T::class.java, handler)
        if (register) handle.register()
        return handle
    }

    fun post(event: Event): Boolean {
        val handlers = subscribers[event::class.java] ?: return false

        for (handler in handlers) {
            if (event.cancelable && event.cancelled) {
                return true
            }

            handler(event)
        }

        return event.cancelable && event.cancelled
    }


    internal fun <T : Event> add(eventClass: Class<T>, handler: (T) -> Unit): (Event) -> Unit {
        val wrapper: (Event) -> Unit = { e -> handler(eventClass.cast(e)) }
        val list = subscribers.getOrPut(eventClass) { mutableListOf() }
        list += wrapper
        return wrapper
    }

    internal fun <T : Event> remove(eventClass: Class<T>, wrapper: (Event) -> Unit) {
        subscribers[eventClass]?.remove(wrapper)
    }

}
