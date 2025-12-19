/*
BSD 3-Clause License
Copyright (c) 2025, Starred
Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
1. Redistributions of source code must retain the above copyright notice, this
   list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.
3. Neither the name of the copyright holder nor the names of its
   contributors may be used to endorse or promote products derived from
   this software without specific prior written permission.
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/


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
