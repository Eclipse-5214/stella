package co.stellarskys.stella.events.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

open class EventBus {
    @PublishedApi
    internal val listeners = ConcurrentHashMap<Class<*>, CopyOnWriteArrayList<EventHandle<*>>>()
    private val cache = ConcurrentHashMap<Class<*>, Array<EventHandle<*>>>()

    inline fun <reified T : Event> on(
        priority: Int = 0,
        ignoreCancelled: Boolean = false,
        register: Boolean = true,
        noinline handler: (T) -> Unit
    ): EventHandle<T> {
        val handle = EventHandle(T::class.java, handler, priority, ignoreCancelled, this, register)
        val list = listeners.computeIfAbsent(T::class.java) { CopyOnWriteArrayList() }
        insertSorted(list, handle)
        rebuildCache(T::class.java)
        return handle
    }

    fun post(event: Event): Boolean {
        cache[event::class.java]?.forEach { handle ->
            @Suppress("UNCHECKED_CAST")
            val typed = handle as EventHandle<Event>
            if (event.cancelled && !typed.ignoreCancelled) return@forEach
            try { typed.handler(event) } catch (_: Exception) {}
        }
        return event.cancelable && event.cancelled
    }

    @PublishedApi
    internal fun rebuildCache(eventClass: Class<*>) {
        val list = listeners[eventClass] ?: return
        cache[eventClass] = list
            .filter { it.registered }
            .sortedByDescending { it.priority }
            .toTypedArray()
    }

    @PublishedApi
    internal fun insertSorted(list: CopyOnWriteArrayList<EventHandle<*>>, handle: EventHandle<*>) {
        val idx = list.indexOfFirst { it.priority < handle.priority }
        if (idx == -1) list.add(handle) else list.add(idx, handle)
    }
}
