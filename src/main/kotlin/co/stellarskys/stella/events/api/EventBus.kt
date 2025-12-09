package co.stellarskys.stella.events.api

import org.jetbrains.annotations.ApiStatus
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

open class EventBus(private val threadSafe: Boolean = true) {
    val subscribers: MutableMap<Class<*>, MutableList<PrioritizedCallback<*>>> =
        if (threadSafe) ConcurrentHashMap() else HashMap()

    private val eventHierarchyCache: MutableMap<Class<*>, List<Class<*>>> =
        if (threadSafe) ConcurrentHashMap() else HashMap()

    inline fun <reified T : Event> register(
        priority: Int = 0,
        add: Boolean = true,
        noinline callback: (T) -> Unit
    ): EventCall {
        return registerInternal(T::class.java, priority, add, callback)
    }

    @JvmOverloads
    fun <T : Event> registerJava(
        eventClass: Class<T>,
        priority: Int = 0,
        add: Boolean = true,
        callback: (T) -> Unit
    ): EventCall {
        return registerInternal(eventClass, priority, add, callback)
    }

    @JvmOverloads
    fun <T : Event> post(event: T, checkHierarchy: Boolean = false): Boolean {
        if (checkHierarchy) return postWithHierarchy(event)

        val handlers = subscribers[event::class.java] ?: return false
        if (handlers.isEmpty()) return false

        handlers.forEach { handler ->
            if (event is CancellableEvent && event.cancelled) {
                return true
            }

            try {
                @Suppress("UNCHECKED_CAST")
                (handler.callback as (T) -> Unit)(event)
            } catch (e: Exception) {
                handleException(event, e)
            }
        }

        return event is CancellableEvent && event.cancelled
    }

    inline fun <reified T : Event> post(supplier: () -> T, checkHierarchy: Boolean = false): Boolean {
        val handlers = if (checkHierarchy) {
            getEventHierarchy(T::class.java).any { subscribers[it]?.isNotEmpty() == true }
        } else {
            subscribers[T::class.java]?.isNotEmpty() == true
        }

        if (!handlers) return false

        val event = supplier()
        return post(event, checkHierarchy)
    }

    private fun <T : Event> postWithHierarchy(event: T): Boolean {
        val eventClasses = getEventHierarchy(event::class.java)
        var wasCancelled = false

        for (eventClass in eventClasses) {
            val handlers = subscribers[eventClass] ?: continue
            if (handlers.isEmpty()) continue

            handlers.forEach { handler ->
                if (event is CancellableEvent && event.cancelled) {
                    return true
                }

                try {
                    @Suppress("UNCHECKED_CAST")
                    (handler.callback as (T) -> Unit)(event)
                } catch (e: Exception) {
                    handleException(event, e)
                }
            }

            if (event is CancellableEvent && event.cancelled) {
                wasCancelled = true
            }
        }

        return wasCancelled
    }

    open fun handleException(event: Any, exception: Exception) {
        exception.printStackTrace()
    }

    fun getSubscriberCount(eventClass: Class<*>): Int {
        return subscribers[eventClass]?.size ?: 0
    }

    fun hasSubscribers(eventClass: Class<*>): Boolean {
        return subscribers[eventClass]?.isNotEmpty() == true
    }

    inline fun <reified T : Event> hasSubscribers(): Boolean {
        return hasSubscribers(T::class.java)
    }

    fun clear() {
        subscribers.clear()
        eventHierarchyCache.clear()
    }

    fun clearEvent(eventClass: Class<*>) {
        subscribers.remove(eventClass)
        eventHierarchyCache.remove(eventClass)
    }

    inline fun <reified T : Event> clearEvent() {
        clearEvent(T::class.java)
    }

    @ApiStatus.Internal
    fun <T : Event> registerInternal(
        eventClass: Class<T>,
        priority: Int,
        add: Boolean,
        callback: (T) -> Unit
    ): EventCall {
        val handlers = subscribers.getOrPut(eventClass) {
            if (threadSafe) CopyOnWriteArrayList() else ArrayList()
        }

        val prioritizedCallback = PrioritizedCallback(priority, callback)

        if (add) {
            addSorted(handlers, prioritizedCallback)
        }

        return EventCallImpl(prioritizedCallback, handlers, this, add)
    }

    @ApiStatus.Internal
    fun <T : Event> addSorted(
        list: MutableList<PrioritizedCallback<*>>,
        callback: PrioritizedCallback<T>
    ) {
        if (list.isEmpty()) {
            list.add(callback)
            return
        }

        val index = list.binarySearch { it.priority.compareTo(callback.priority) }
        val insertIndex = if (index < 0) -(index + 1) else index
        list.add(insertIndex, callback)
    }

    @ApiStatus.Internal
    fun getEventHierarchy(eventClass: Class<*>): List<Class<*>> {
        return eventHierarchyCache.getOrPut(eventClass) {
            val hierarchy = mutableListOf<Class<*>>()
            var current: Class<*>? = eventClass

            while (current != null && Event::class.java.isAssignableFrom(current)) {
                hierarchy.add(current)
                current = current.superclass
            }

            hierarchy
        }
    }
}