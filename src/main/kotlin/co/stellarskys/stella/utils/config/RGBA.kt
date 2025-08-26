package co.stellarskys.stella.utils.config
import java.awt.Color

/**
 * Represents a color using red, green, blue, and alpha components.
 *
 * This class provides utility functions for converting between RGBA and other color formats
 * like Java AWT's `Color`, hexadecimal strings, and HSVA (hue, saturation, value, alpha).
 *
 * @property r Red component (0–255)
 * @property g Green component (0–255)
 * @property b Blue component (0–255)
 * @property a Alpha component (0–255), defaults to 255 (fully opaque)
 */
data class RGBA(val r: Int, val g: Int, val b: Int, val a: Int = 255) {

    /**
     * Converts this RGBA color to a [Color] object.
     *
     * @param includeAlpha If true, includes the alpha component in the resulting color.
     * @return A [Color] instance representing this RGBA color.
     */
    fun toColor(includeAlpha: Boolean = true): Color {
        return if (includeAlpha) {
            Color(r, g, b, a)
        } else {
            Color(r, g, b)
        }
    }

    /**
     * Converts this color to a hexadecimal string.
     *
     * @param includeAlpha If true, includes the alpha component (e.g., `#rrggbbaa`); otherwise `#rrggbb`.
     * @return The hex string representation of this color.
     */
    fun toHex(includeAlpha: Boolean = true): String {
        return if (includeAlpha) {
            String.format("#%02x%02x%02x%02x", r, g, b, a)
        } else {
            String.format("#%02x%02x%02x", r, g, b)
        }
    }

    /**
     * Converts this RGBA color to an HSVA array.
     *
     * @return A float array of size 4: `[hue, saturation, value, alpha]`, where alpha is normalized (0.0–1.0).
     */
    fun toHSVA(): FloatArray {
        val hsv = Color.RGBtoHSB(r, g, b, null)
        return floatArrayOf(hsv[0], hsv[1], hsv[2], a / 255f)
    }

    /**
     * Converts this RGBA color to a packed ARGB Int.
     *
     * @return An Int representing the color in 0xAARRGGBB format.
     */
    fun toColorInt(): Int {
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }

    companion object {

        /**
         * Parses a hex color string and returns an [RGBA] instance.
         *
         * Accepts the following formats:
         * - `#rgb`
         * - `#rgba`
         * - `#rrggbb`
         * - `#rrggbbaa`
         *
         * @param hex The hex string to parse.
         * @return An [RGBA] color corresponding to the input.
         * @throws IllegalArgumentException If the hex string is malformed.
         */
        fun fromHex(hex: String): RGBA {
            val cleaned = hex.trim().lowercase().removePrefix("#")

            val expanded = when (cleaned.length) {
                3 -> cleaned.map { "$it$it" }.joinToString("") + "ff"
                4 -> cleaned.map { "$it$it" }.joinToString("")
                6, 8 -> cleaned
                else -> throw IllegalArgumentException("Invalid hex color: $hex")
            }

            val r = expanded.substring(0, 2).toInt(16)
            val g = expanded.substring(2, 4).toInt(16)
            val b = expanded.substring(4, 6).toInt(16)
            val a = if (expanded.length == 8) expanded.substring(6, 8).toInt(16) else 255

            return RGBA(r, g, b, a)
        }

        /**
         * Converts HSVA (hue, saturation, value, alpha) to an [RGBA] instance.
         *
         * @param hue Hue component (0.0–1.0)
         * @param saturation Saturation (0.0–1.0)
         * @param value Brightness (0.0–1.0)
         * @param alpha Alpha transparency (0.0–1.0), defaults to 1.0 (opaque)
         * @return An [RGBA] representing the converted color.
         */
        fun fromHSVA(hue: Float, saturation: Float, value: Float, alpha: Float = 1f): RGBA {
            val rgb = Color.getHSBColor(hue, saturation, value)
            val a = (alpha.coerceIn(0f, 1f) * 255).toInt()
            return RGBA(rgb.red, rgb.green, rgb.blue, a)
        }
    }
}