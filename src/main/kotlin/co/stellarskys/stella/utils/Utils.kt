package co.stellarskys.stella.utils

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.UIRoundedRectangle
import org.apache.commons.lang3.SystemUtils
import kotlin.math.sqrt

object Utils {
    fun createBlock(radius: Float = 0f): UIComponent {
        return if (SystemUtils.IS_OS_MAC_OSX) UIBlock() else UIRoundedRectangle(radius)
    }

    /**
     * Calculates the Euclidean distance between two 3D points.
     *
     * @param a First point as Triple(x, y, z)
     * @param b Second point as Triple(x, y, z)
     * @return The straight-line distance between the points.
     */
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
    fun calcDistance(a: Triple<Int, Int, Int>, b: Triple<Int, Int, Int>): Double {
        val dx = (a.first - b.first).toDouble()
        val dy = (a.second - b.second).toDouble()
        val dz = (a.third - b.third).toDouble()
        return dx * dx + dy * dy + dz * dz
    }
}