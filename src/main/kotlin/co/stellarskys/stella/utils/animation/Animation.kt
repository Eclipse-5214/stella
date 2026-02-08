package co.stellarskys.stella.utils.animation

import co.stellarskys.stella.events.EventBus
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
    private val target  = DoubleArray(4)
    private var base = DoubleArray(4)
    private var peak = DoubleArray(4)
    private var pulsing = false
    private var pulseUp = true
    private var init = false
    private var currentFrame = 0L

    @Suppress("UNCHECKED_CAST")
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        val frame = EventBus.currentFrame
        if (currentFrame != frame) {
            val channels = if (isColor) 4 else 1
            for (i in 0 until channels) current[i] = calculateStep(current[i], target[i])

            // Pulse logic
            if (pulsing && done()) {
                if (pulseUp) {
                    pulseUp = false
                    for (i in 0 until 4) target[i] = base[i]
                } else {
                    pulsing = false
                }
            }

            currentFrame = frame
        }


        return if (isColor) {
            Color(current[0].toInt().coerceIn(0, 255), current[1].toInt().coerceIn(0, 255), current[2].toInt().coerceIn(0, 255), current[3].toInt().coerceIn(0, 255)) as T
        } else {
            when (property.returnType.classifier) {
                Float::class -> current[0].toFloat() as T
                Double::class -> current[0] as T
                else -> current[0].toInt() as T
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun getTarget(property: KProperty<*>): T {
        return if (isColor) {
            Color(
                target[0].toInt().coerceIn(0, 255),
                target[1].toInt().coerceIn(0, 255),
                target[2].toInt().coerceIn(0, 255),
                target[3].toInt().coerceIn(0, 255)
            ) as T
        } else {
            when (property.returnType.classifier) {
                Float::class -> target[0].toFloat() as T
                Double::class -> target[0] as T
                else -> target[0].toInt() as T
            }
        }
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        if (isColor) {
            value as Color
            target[0] = value.red.toDouble()
            target[1] = value.green.toDouble()
            target[2] = value.blue.toDouble()
            target[3] = value.alpha.toDouble()
        } else {
            target[0] = (value as Number).toDouble()
        }

        if (!init) { snap(); init = true }
    }

    private fun calculateStep(current: Double, target: Double): Double {
        val delta = target - current
        if (abs(delta) < error) return target

        val step = when (type) {
            AnimType.LINEAR -> delta * coeff
            AnimType.EASE_OUT -> delta * (1 - (1 - coeff).pow(2))
            AnimType.EASE_IN -> delta * coeff.pow(2)
            AnimType.EASE_IN_OUT -> delta * (0.5 - 0.5 * cos(Math.PI * coeff))
            AnimType.SMOOTH -> delta * ((1 - cos(Math.PI * coeff)) / 2)
            AnimType.SPRING -> delta * coeff * 1.2 + sin(delta * 10) * coeff * 0.05
        }

        val next = current + step
        return if (abs(target - next) < error) target else next
    }

    fun done(eps: Double = error) = (0 until 4).all { abs(current[it] - target[it]) <= eps }
    fun snap() { for (i in 0 until 4) current[i] = target[i] }

    fun pulse(peakValue: T) {
        for (i in 0 until 4) base[i] = current[i]

        if (isColor) {
            peakValue as Color
            peak[0] = peakValue.red.toDouble()
            peak[1] = peakValue.green.toDouble()
            peak[2] = peakValue.blue.toDouble()
            peak[3] = peakValue.alpha.toDouble()
        } else {
            peak[0] = (peakValue as Number).toDouble()
        }

        pulsing = true
        pulseUp = true

        for (i in 0 until 4) target[i] = peak[i]
    }
}