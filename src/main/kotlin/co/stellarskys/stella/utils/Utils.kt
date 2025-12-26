package co.stellarskys.stella.utils

import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.player
import net.minecraft.core.BlockPos
import net.minecraft.network.chat.Component
import net.minecraft.sounds.SoundEvent
import kotlin.math.sqrt

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
}