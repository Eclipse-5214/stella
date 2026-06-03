package co.stellarskys.stella.api.horizon.animation

import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.SimpleTimeMark
import co.stellarskys.stella.api.handlers.Chronos.isZero

class Interpolator<T>(private val lerp: (delta: Double, start: T, end: T) -> T) {
    private var state: Pair<Node<T>, Node<T>>? = null
    class Node<T>(val value: T, val time: SimpleTimeMark = Chronos.now)
    fun update(newValue: T) { state = Pair(state?.second ?: Node(newValue), Node(newValue)) }

    fun get(): T? {
        val (prev, next) = state ?: return null
        val total = next.time - prev.time
        return if (total.isZero) next.value else lerp((next.time.since / total).coerceIn(0.0, 1.0), prev.value, next.value)
    }
}