package co.stellarskys.stella.utils

//#if MC >= 1.21.5
import net.minecraft.util.Formatting
//#endif

import org.apache.commons.lang3.StringUtils as ApacheStringUtils

fun CharSequence?.countMatches(subString: CharSequence): Int = ApacheStringUtils.countMatches(this, subString)

//#if MC >= 1.21.5
fun String.stripControlCodes(): String = Formatting.strip(this)!!
//#endif

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
private val emoteRegex = "[^\\u0000-\\u007F]".toRegex()

fun String.clearCodes(): String = this.replace(removeCodesRegex, "")
fun String.removeEmotes(): String = this.replace(emoteRegex, "")