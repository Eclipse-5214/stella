package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.FontDescription
import net.minecraft.resources.ResourceLocation
import net.minecraft.sounds.SoundEvent
import java.awt.Color
import kotlin.math.sqrt
import kotlin.reflect.KProperty

object Utils {
    /**
     * Calculates the Euclidean distance between two 3D points.
     *
     * @param a First point as Triple(x, y, z)
     * @param b Second point as Triple(x, y, z)
     * @return The straight-line distance between the points.
     */

    fun calcDistanceSq(pos1: BlockPos, pos2: BlockPos) = calcDistanceSq(Triple(pos1.x, pos1.y, pos1.z), Triple(pos2.x, pos2.y, pos2.z))
    fun calcDistanceSq(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Double {
        val dx = (a.first - b.first).toDouble()
        val dy = (a.second - b.second).toDouble()
        val dz = (a.third - b.third).toDouble()
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    /**
     * Calculates the squared Euclidean distance between two 3D points.
     * Faster than [calcDistanceSq] because it avoids the square root.
     */
    fun calcDistance(pos1: BlockPos, pos2: BlockPos) = calcDistance(Triple(pos1.x, pos1.y, pos1.z), Triple(pos2.x, pos2.y, pos2.z))
    fun calcDistance(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Double {
        val dx = (a.first - b.first).toDouble()
        val dy = (a.second - b.second).toDouble()
        val dz = (a.third - b.third).toDouble()
        return dx * dx + dy * dy + dz * dz
    }

    // Linear interpolation with clamped factor
    fun lerp(f: Double, a: Double, b: Double): Double {
        val t = f.coerceIn(0.0, 1.0)
        return a + (b - a) * t
    }

    // Angle interpolation (shortest path), in radians
    fun lerpAngle(f: Double, a: Double, b: Double): Double {
        var diff = (b - a) % 360.0
        if (diff < -180.0) diff += 360.0
        if (diff > 180.0) diff -= 360.0
        val t = f.coerceIn(0.0, 1.0)
        return a + diff * t
    }

    fun mapRange(n: Double, inMin: Double, inMax: Double, outMin: Double, outMax: Double): Double = (n - inMin) * (outMax - outMin) / (inMax - inMin) + outMin

    /**
     * Converts a java.awt.Color to a Hex string.
     * @param includeAlpha If true, returns #rrggbbaa; otherwise #rrggbb.
     */
    fun Color.toHex(includeAlpha: Boolean = true): String {
        return if (includeAlpha) {
            String.format("#%02x%02x%02x%02x", red, green, blue, alpha)
        } else {
            String.format("#%02x%02x%02x", red, green, blue)
        }
    }

    /**
     * Parses a hex string into a java.awt.Color.
     * Supports #rgb, #rgba, #rrggbb, and #rrggbbaa.
     */
    fun colorFromHex(hex: String): Color {
        val cleaned = hex.trim().lowercase().removePrefix("#")

        val expanded = when (cleaned.length) {
            3 -> cleaned.map { "$it$it" }.joinToString("") + "ff"
            4 -> cleaned.map { "$it$it" }.joinToString("")
            6 -> cleaned + "ff"
            8 -> cleaned
            else -> throw IllegalArgumentException("Invalid hex color: $hex")
        }

        val r = expanded.take(2).toInt(16)
        val g = expanded.substring(2, 4).toInt(16)
        val b = expanded.substring(4, 6).toInt(16)
        val a = expanded.substring(6, 8).toInt(16)

        return Color(r, g, b, a)
    }

    fun Color.getNormalized(): FloatArray = this.getRGBComponents(null)

    fun decodeRoman(roman: String): Int {
        val values = mapOf(
            'I' to 1,
            'V' to 5,
            'X' to 10,
            'L' to 50,
            'C' to 100,
            'D' to 500,
            'M' to 1000
        )

        var total = 0
        var prev = 0

        for (char in roman.uppercase()) {
            val value = values[char] ?: return 0
            total += if (value > prev) {
                value - 2 * prev
            } else {
                value
            }
            prev = value
        }

        return total
    }

    fun alert(title: String, sound: SoundEvent? = null, volume: Float = 1f, pitch: Float = 1f) {
        client.gui.setTimes(0, 20, 5)
        client.gui.setTitle(Component.literal(title))

        if (sound == null) return
        player?.playSound(sound, volume, pitch)
    }

    object Fonts {
        val montserrat_bold = getFont("montserrat")

        fun getFont(font: String): FontDescription {
            val resource = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, font)
            return FontDescription.Resource(resource)
        }
    }

    inline fun <reified T : Any> lerped(coeff: Double = 0.2) = LerpedDelegate<T>(coeff, isColor = (T::class == Color::class))

    class LerpedDelegate<T : Any>(
        private val coeff: Double,
        private val isColor: Boolean
    ) {
        private val current = DoubleArray(4)
        private val target  = DoubleArray(4)
        private var init = false
        private var lastFrame = -1f


        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val frameTime = client.deltaTracker.getGameTimeDeltaPartialTick(true)

            if (lastFrame != -1f && frameTime != lastFrame) {
                val diff = (frameTime - lastFrame).toDouble()
                val frameCoeff = (coeff * diff).coerceIn(0.0, 1.0)

                val channels = if (isColor) 4 else 1
                for (i in 0 until channels) {
                    current[i] += (target[i] - current[i]) * frameCoeff
                }
            }
            lastFrame = frameTime

            return createValue(property)
        }

        @Suppress("UNCHECKED_CAST")
        private fun createValue(property: KProperty<*>): T {
            return if (isColor) {
                Color(
                    current[0].toInt().coerceIn(0, 255),
                    current[1].toInt().coerceIn(0, 255),
                    current[2].toInt().coerceIn(0, 255),
                    current[3].toInt().coerceIn(0, 255)
                ) as T
            } else {
                when (property.returnType.classifier) {
                    Float::class -> current[0].toFloat() as T
                    Double::class -> current[0] as T
                    else -> current[0].toInt() as T
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

        fun done() = current[0] == target[0] && current[1] == target[1] && current[2] == target[2] && current[3] == target[3]
        fun snap() { for (i in 0 until 4) current[i] = target[i] }
    }
}