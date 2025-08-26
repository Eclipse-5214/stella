package co.stellarskys.stella.utils

import net.minecraft.util.Formatting
import org.apache.commons.lang3.StringUtils as ApacheStringUtils

fun CharSequence?.countMatches(subString: CharSequence): Int = ApacheStringUtils.countMatches(this, subString)

fun String.stripControlCodes(): String = Formatting.strip(this)!!

fun CharSequence?.startsWithAny(vararg sequences: CharSequence?) = ApacheStringUtils.startsWithAny(this, *sequences)
fun CharSequence.startsWithAny(sequences: Iterable<CharSequence>): Boolean = sequences.any { startsWith(it) }
fun CharSequence?.containsAny(vararg sequences: CharSequence?): Boolean {
    if (this == null) return false
    return sequences.any { it != null && this.contains(it) }
}

fun String.toDashedUUID(): String {
    if (this.length != 32) return this
    return buildString {
        append(this@toDashedUUID)
        insert(20, "-")
        insert(16, "-")
        insert(12, "-")
        insert(8, "-")
    }
}

fun String.toTitleCase(): String = this.lowercase().replaceFirstChar { c -> c.titlecase() }
fun String.splitToWords(): String = this.split('_', ' ').joinToString(" ") { it.toTitleCase() }
fun String.isInteger(): Boolean = this.toIntOrNull() != null

private val removeCodesRegex = "[\\u00a7&][0-9a-fk-or]".toRegex()

fun String.clearCodes(): String = this.replace(removeCodesRegex, "")