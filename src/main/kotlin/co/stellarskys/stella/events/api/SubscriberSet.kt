package co.stellarskys.stella.events.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

data class SubscriberSet(
    val all: ConcurrentHashMap<Class<*>, ConcurrentSkipListSet<EventHandle<*>>> = ConcurrentHashMap(),
    val enabled: ConcurrentHashMap<Class<*>, Array<EventHandle<*>>> = ConcurrentHashMap()
) {
    fun setup(eventClass: Class<*>): ConcurrentSkipListSet<EventHandle<*>> = all.computeIfAbsent(eventClass) { ConcurrentSkipListSet{ a, b -> b.priority.compareTo(a.priority).takeIf { it != 0 } ?: if (a === b) 0 else 1 } }
}