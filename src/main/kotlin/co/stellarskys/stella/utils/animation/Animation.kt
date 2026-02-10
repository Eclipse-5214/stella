package co.stellarskys.stella.utils.animation

import co.stellarskys.stella.Stella
import java.awt.Color
import kotlin.math.*
import kotlin.reflect.KProperty

class Animation<T : Any>(
    private val coeff: Double,
    private val error: Double = 0.001,
    private val type: AnimType = AnimType.LINEAR,
    private val isColor: Boolean
) {
    private val current = DoubleArray(4)
    private val target = DoubleArray(4)
    private var base = DoubleArray(4)
    private var peak = DoubleArray(4)
    private var pulsing = false
    private var pulseUp = true
    private var init = false

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val delta = Stella.DELTA.currentDelta

        val channels = if (isColor) 4 else 1
        for (i in 0 until channels) {
            current[i] = calculateStep(current[i], target[i], delta)
        }

        if (pulsing && done()) {
            if (pulseUp) {
                pulseUp = false
                // Load base (starting point) back into target to return home
                for (i in 0 until 4) target[i] = base[i]
            } else {
                pulsing = false
            }
        }

        return getArray(current, property)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        setArray(target, value)
        if (!init) {
            snap()
            init = true
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getTarget(property: KProperty<*>): T = getArray(target, property)

    private fun calculateStep(current: Double, target: Double, delta: Double): Double {
        val diff = target - current
        if (abs(diff) < error) return target
        val adjustedCoeff = (1.0 - (1.0 - coeff).pow(delta)).coerceIn(0.0, 1.0)

        val step = when (type) {
            AnimType.LINEAR -> diff * adjustedCoeff
            AnimType.EASE_OUT -> diff * (1.0 - (1.0 - adjustedCoeff).pow(2.0))
            AnimType.EASE_IN -> diff * adjustedCoeff.pow(2.0)
            AnimType.EASE_IN_OUT -> diff * (0.5 - 0.5 * cos(PI * adjustedCoeff))
            AnimType.SMOOTH -> diff * ((1.0 - cos(PI * adjustedCoeff)) / 2.0)
            AnimType.SPRING -> {
                // Keep the spring tension relative to the delta-adjusted coefficient
                val tension = adjustedCoeff * 1.2
                val oscillation = sin(diff * 10.0) * (adjustedCoeff * 0.05)
                diff * tension + oscillation
            }
        }

        val next = current + step
        return if (abs(target - next) < error) target else next
    }

    fun done(eps: Double = error) = (0 until 4).all { abs(current[it] - target[it]) <= eps }
    fun snap() { for (i in 0 until 4) current[i] = target[i] }

    fun pulse(peakValue: T) {
        if (pulsing) return

        for (i in 0 until 4) base[i] = current[i]
        setArray(peak, peakValue)

        pulsing = true
        pulseUp = true

        for (i in 0 until 4) target[i] = peak[i]
    }

    @Suppress("UNCHECKED_CAST")
    private fun getArray(array: DoubleArray, property: KProperty<*>): T {
        return if (isColor) {
            Color(
                array[0].toInt().coerceIn(0, 255),
                array[1].toInt().coerceIn(0, 255),
                array[2].toInt().coerceIn(0, 255),
                array[3].toInt().coerceIn(0, 255)
            ) as T
        } else {
            when (property.returnType.classifier) {
                Float::class -> array[0].toFloat() as T
                Double::class -> array[0] as T
                Int::class -> array[0].toInt() as T
                else -> array[0] as T
            }
        }
    }

    private fun setArray(array: DoubleArray, value: T) {
        if (isColor) {
            val color = value as Color
            array[0] = color.red.toDouble()
            array[1] = color.green.toDouble()
            array[2] = color.blue.toDouble()
            array[3] = color.alpha.toDouble()
        } else {
            array[0] = (value as Number).toDouble()
        }
    }
}