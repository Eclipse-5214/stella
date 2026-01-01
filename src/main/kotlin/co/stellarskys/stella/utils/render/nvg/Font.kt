package co.stellarskys.stella.utils.render.nvg

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/*
 * Adapted from Font.kt in OdinFabric
 * https://github.com/odtheking/OdinFabric
 *
 * BSD 3-Clause License
 * Copyright (c) 2025, odtheking
 * See full license at: https://opensource.org/licenses/BSD-3-Clause
 */

class Font {
    val name: String
    private val cachedBytes: ByteArray?

    constructor(name: String, inputStream: InputStream) {
        this.name = name
        this.cachedBytes = inputStream.use { it.readBytes() }
    }

    fun buffer(): ByteBuffer {
        val bytes = cachedBytes ?: throw IllegalStateException("Font bytes not cached for font: $name")

        return ByteBuffer.allocateDirect(bytes.size)
            .order(ByteOrder.nativeOrder())
            .put(bytes)
            .flip() as ByteBuffer
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Font && name == other.name
    }
}