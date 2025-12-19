package co.stellarskys.stella.events.api

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListSet

data class SubscriberSet(
    val all: ConcurrentHashMap<Class<*>, ConcurrentSkipListSet<EventHandle<*>>> = ConcurrentHashMap(),
    val enabled: ConcurrentHashMap<Class<*>, List<EventHandle<out Event>>> = ConcurrentHashMap()
) {
    fun setup(eventClass: Class<*>): ConcurrentSkipListSet<EventHandle<*>> = all.computeIfAbsent(eventClass) { ConcurrentSkipListSet(compareByDescending { it.priority }) }
}