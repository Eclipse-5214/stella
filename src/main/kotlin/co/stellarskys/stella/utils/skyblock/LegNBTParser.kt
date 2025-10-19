package co.stellarskys.stella.utils.skyblock

import net.minecraft.nbt.AbstractNbtNumber
import net.minecraft.nbt.NbtByte
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtDouble
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtFloat
import net.minecraft.nbt.NbtInt
import net.minecraft.nbt.NbtList
import net.minecraft.nbt.NbtLong
import net.minecraft.nbt.NbtShort
import net.minecraft.nbt.NbtString
import java.util.Stack

/*
 * Adapted from Firmarment
 * Under GPL 3.0 License
 */

object LegNBTParser {
    class TagParsingException(val base: String, val offset: Int, message: String) :
        Exception("$message at $offset in `$base`")

    private class StringRacer(val input: String) {
        var index = 0
        val stack = Stack<Int>()

        fun pushState() = stack.push(index)
        fun popState() = run { index = stack.pop() }
        fun discardState() = stack.pop()

        fun peek(n: Int) = input.substring(index.coerceAtMost(input.length), (index + n).coerceAtMost(input.length))
        fun peekReq(n: Int) = peek(n).takeIf { it.length == n }
        fun consume(n: Int) = peekReq(n)?.also { index += n }
        fun tryConsume(s: String) = peek(s.length) == s && run { index += s.length; true }
        fun consumeWhile(pred: (String) -> Boolean): String {
            var acc = ""
            while (!finished() && pred(acc + peek(1))) {
                acc += consume(1)
            }
            return acc
        }

        fun expect(s: String, msg: String) {
            if (!tryConsume(s)) error(msg)
        }

        fun error(msg: String): Nothing = throw TagParsingException(input, index, msg)
        fun finished() = index >= input.length
    }

    fun parse(raw: String): NbtCompound = LegNBTParser(raw).baseTag

    private class LegNBTParser(raw: String) {
        val racer = StringRacer(raw)
        val baseTag = parseTag()

        companion object {
            val digitRange = "0123456789-"
            object Patterns {
                val DOUBLE = "([-+]?[0-9]*\\.?[0-9]+)[dD]".toRegex()
                val FLOAT = "([-+]?[0-9]*\\.?[0-9]+)[fF]".toRegex()
                val BYTE = "([-+]?[0-9]+)[bB]".toRegex()
                val LONG = "([-+]?[0-9]+)[lL]".toRegex()
                val SHORT = "([-+]?[0-9]+)[sS]".toRegex()
                val INTEGER = "([-+]?[0-9]+)".toRegex()
                val DOUBLE_UNTYPED = "([-+]?[0-9]*\\.?[0-9]+)".toRegex()
                val ROUGH = "[-+]?[0-9]*\\.?[0-9]*[dDbBfFlLsS]?".toRegex()
            }
        }

        private fun skipWhitespace() {
            racer.consumeWhile { it.last().isWhitespace() }
        }

        private fun parseTag(): NbtCompound {
            skipWhitespace()
            racer.expect("{", "Expected '{'")
            val tag = NbtCompound()
            while (!racer.tryConsume("}")) {
                skipWhitespace()
                val key = parseIdentifier()
                skipWhitespace()
                racer.expect(":", "Expected ':' after key")
                skipWhitespace()
                val value = parseAny()
                tag.put(key, value)
                racer.tryConsume(",")
                skipWhitespace()
            }
            return tag
        }

        private fun parseAny(): NbtElement {
            skipWhitespace()
            val c = racer.peekReq(1) ?: racer.error("Unexpected EOF")
            return when {
                c == "{" -> parseTag()
                c == "[" -> parseList()
                c == "\"" -> parseStringTag()
                c.first() in digitRange -> parseNumericTag()
                else -> racer.error("Unexpected token '$c'")
            }
        }

        private fun parseList(): NbtList {
            skipWhitespace()
            racer.expect("[", "Expected '['")
            val list = NbtList()
            while (!racer.tryConsume("]")) {
                skipWhitespace()
                racer.pushState()
                val prefix = racer.consumeWhile { it.all { ch -> ch in digitRange } }
                skipWhitespace()
                if (!racer.tryConsume(":") || prefix.isEmpty()) {
                    racer.popState()
                    list.add(parseAny())
                } else {
                    racer.discardState()
                    skipWhitespace()
                    list.add(parseAny())
                }
                skipWhitespace()
                racer.tryConsume(",")
            }
            return list
        }

        private fun parseQuotedString(): String {
            skipWhitespace()
            racer.expect("\"", "Expected '\"'")
            val sb = StringBuilder()
            while (true) {
                val c = racer.consume(1) ?: racer.error("Unterminated string")
                when (c) {
                    "\"" -> break
                    "\\" -> {
                        val esc = racer.consume(1) ?: racer.error("Unfinished escape")
                        if (esc != "\"" && esc != "\\") {
                            racer.index--
                            racer.error("Invalid escape '\\$esc'")
                        }
                        sb.append(esc)
                    }
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }

        private fun parseStringTag(): NbtString = NbtString.of(parseQuotedString())

        private fun parseNumericTag(): AbstractNbtNumber {
            skipWhitespace()
            val raw = racer.consumeWhile { Patterns.ROUGH.matchEntire(it) != null }
            if (raw.isEmpty()) racer.error("Expected numeric value")

            return when {
                Patterns.FLOAT.matches(raw) -> NbtFloat.of(Patterns.FLOAT.matchEntire(raw)!!.groupValues[1].toFloat())
                Patterns.BYTE.matches(raw) -> NbtByte.of(Patterns.BYTE.matchEntire(raw)!!.groupValues[1].toByte())
                Patterns.LONG.matches(raw) -> NbtLong.of(Patterns.LONG.matchEntire(raw)!!.groupValues[1].toLong())
                Patterns.SHORT.matches(raw) -> NbtShort.of(Patterns.SHORT.matchEntire(raw)!!.groupValues[1].toShort())
                Patterns.INTEGER.matches(raw) -> NbtInt.of(Patterns.INTEGER.matchEntire(raw)!!.groupValues[1].toInt())
                Patterns.DOUBLE.matches(raw) -> NbtDouble.of(Patterns.DOUBLE.matchEntire(raw)!!.groupValues[1].toDouble())
                Patterns.DOUBLE_UNTYPED.matches(raw) -> NbtDouble.of(Patterns.DOUBLE_UNTYPED.matchEntire(raw)!!.groupValues[1].toDouble())
                else -> racer.error("Unrecognized numeric format '$raw'")
            }
        }

        private fun parseIdentifier(): String {
            skipWhitespace()
            return if (racer.peek(1) == "\"") parseQuotedString()
            else racer.consumeWhile { it.last() != ':' && !it.last().isWhitespace() }
        }
    }
}
