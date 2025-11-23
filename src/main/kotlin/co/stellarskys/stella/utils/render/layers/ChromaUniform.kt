package co.stellarskys.stella.utils.render.layers

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.buffers.Std140Builder
import com.mojang.blaze3d.buffers.Std140SizeCalculator
import net.minecraft.client.renderer.DynamicUniformStorage
import net.minecraft.client.renderer.DynamicUniformStorage.DynamicUniform
import java.lang.AutoCloseable
import java.nio.ByteBuffer

/**
 * Chroma related codes adapted from SkyHanni under LGPL-2.1 license
 * @link [github.com/hannibal002/SkyHanni/blob/beta/LICENSE](https://github.com/hannibal002/SkyHanni/blob/beta/LICENSE)
 * @author hannibal2
 */
class ChromaUniform : AutoCloseable {
    private val UNIFORM_SIZE = Std140SizeCalculator().putFloat().putFloat().putFloat().get()

    private val storage = DynamicUniformStorage<UniformValue?>("SBA Chroma UBO", UNIFORM_SIZE, 2)

    fun writeWith(chromaSize: Float?, timeOffset: Float?, saturation: Float?): GpuBufferSlice {
        return storage.writeUniform(
            UniformValue(chromaSize, timeOffset, saturation)
        )
    }

    // Imperative to clear DynamicUniformStorage every frame.
    // Handled in RendersystemMixin.
    fun endFrame() {
        storage.endFrame()
    }

    override fun close() {
        storage.close()
    }

    internal data class UniformValue(val chromaSize: Float?, val timeOffset: Float?, val saturation: Float?) :
        DynamicUniform {
        override fun write(buffer: ByteBuffer) {
            Std140Builder.intoBuffer(buffer)
                .putFloat(chromaSize!!)
                .putFloat(timeOffset!!)
                .putFloat(saturation!!)
        }
    }
}