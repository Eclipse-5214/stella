package co.stellarskys.stella.api.handlers

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

object Star { internal val reaction = ThreadLocal<(() -> Unit)?>() }
interface Fuel { fun stack(dependent: () -> Unit); fun burn() }

class Spark<T>(initial: T) : ReadWriteProperty<Any?, T>, Fuel {
    private val dependents = mutableSetOf<() -> Unit>()
    private var _value = initial

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        Star.reaction.get()?.let { stack(it) }
        return _value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (_value != value) { _value = value; burn() }
    }

    override fun stack(dependent: () -> Unit) { dependents.add(dependent) }
    override fun burn() { dependents.forEach { it() } }
}

class Flare<T>(initial: T, private val onInvalidate: () -> T) : ReadWriteProperty<Any?, T>, Fuel {
    private val dependents = mutableSetOf<() -> Unit>()
    private var _value = initial

    private val ignite = object : () -> Unit {
        override fun invoke() {
            Star.reaction.set(this)
            val newValue = onInvalidate()
            Star.reaction.set(null)

            if (_value != newValue) { _value = newValue; burn() }
        }
    }

    init { ignite() }

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val active = Star.reaction.get()
        if (active != null && active != ignite) { stack(active) }
        return _value
    }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) { _value = value }
    override fun stack(dependent: () -> Unit) { dependents.add(dependent) }
    override fun burn() { dependents.forEach { it() } }
}