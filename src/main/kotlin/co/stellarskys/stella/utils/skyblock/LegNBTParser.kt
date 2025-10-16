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

object LegNBTParser {
    class TagParsingException(val base: String, val offset: Int, message: String) :
        Exception("$message at $offset in `$base`")

    private class StringRacer(val input: String) {
        var index = 0
        val stack = Stack<Int>()

        fun peek(n: Int) = input.substring(index.coerceAtMost(input.length), (index + n).coerceAtMost(input.length))
        fun consume(n: Int): String? = peek(n).takeIf { it.length == n }?.also { index += n }
        fun tryConsume(s: String): Boolean = peek(s.length) == s && run { index += s.length; true }
        fun consumeWhile(predicate: (Char) -> Boolean): String {
            val start = index
            while (index < input.length && predicate(input[index])) index++
            return input.substring(start, index)
        }

        fun expect(s: String, msg: String) {
            if (!tryConsume(s)) throw TagParsingException(input, index, msg)
        }

        fun finished() = index >= input.length
    }

    fun parse(raw: String): NbtCompound {
        val racer = StringRacer(raw)
        skipWhitespace(racer)
        racer.expect("{", "Expected '{'")
        val compound = NbtCompound()
        while (!racer.tryConsume("}")) {
            skipWhitespace(racer)
            val key = parseIdentifier(racer)
            skipWhitespace(racer)
            racer.expect(":", "Expected ':' after key")
            skipWhitespace(racer)
            val value = parseValue(racer)
            compound.put(key, value)
            racer.tryConsume(",")
        }
        return compound
    }

    private fun skipWhitespace(r: StringRacer) {
        r.consumeWhile { it.isWhitespace() }
    }

    private fun parseIdentifier(r: StringRacer): String {
        return if (r.peek(1) == "\"") parseQuotedString(r) else r.consumeWhile { it != ':' && !it.isWhitespace() }
    }

    private fun parseQuotedString(r: StringRacer): String {
        r.expect("\"", "Expected '\"'")
        val sb = StringBuilder()
        while (true) {
            val c = r.consume(1) ?: throw TagParsingException(r.input, r.index, "Unterminated string")
            when (c) {
                "\"" -> break
                "\\" -> {
                    val esc = r.consume(1) ?: throw TagParsingException(r.input, r.index, "Unfinished escape")
                    sb.append(esc)
                }
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun parseValue(r: StringRacer): NbtElement {
        return when (val c = r.peek(1)) {
            "{" -> parse(raw = r.input.substring(r.index))
            "[" -> parseList(r)
            "\"" -> NbtString.of(parseQuotedString(r))
            else -> parseNumber(r)
        }
    }

    private fun parseList(r: StringRacer): NbtList {
        r.expect("[", "Expected '['")
        val list = NbtList()
        while (!r.tryConsume("]")) {
            skipWhitespace(r)
            val prefix = r.consumeWhile { it.isDigit() || it == '-' }
            if (prefix.isNotEmpty() && r.tryConsume(":")) {
                skipWhitespace(r)
            }
            list.add(parseValue(r))
            r.tryConsume(",")
        }
        return list
    }

    private fun parseNumber(r: StringRacer): AbstractNbtNumber {
        val raw = r.consumeWhile { it.isDigit() || it == '.' || it == '-' }
        return when {
            raw.endsWith("b", true) -> NbtByte.of(raw.dropLast(1).toByte())
            raw.endsWith("s", true) -> NbtShort.of(raw.dropLast(1).toShort())
            raw.endsWith("l", true) -> NbtLong.of(raw.dropLast(1).toLong())
            raw.endsWith("f", true) -> NbtFloat.of(raw.dropLast(1).toFloat())
            raw.endsWith("d", true) -> NbtDouble.of(raw.dropLast(1).toDouble())
            raw.contains('.') -> NbtDouble.of(raw.toDouble())
            else -> NbtInt.of(raw.toInt())
        }
    }
}