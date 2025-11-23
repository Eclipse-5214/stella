package co.stellarskys.stella.utils.render.layers

import com.mojang.blaze3d.buffers.GpuBufferSlice
import com.mojang.blaze3d.systems.RenderPass
import xyz.meowing.knit.api.KnitClient.client
import xyz.meowing.knit.api.scheduler.TickScheduler

/**
 * Chroma related codes adapted from SkyHanni under LGPL-2.1 license
 * @link [github.com/hannibal002/SkyHanni/blob/beta/LICENSE](https://github.com/hannibal002/SkyHanni/blob/beta/LICENSE)
 * @author hannibal2
 */
object GuiRendererHook {
    var chromaBufferSlice: GpuBufferSlice? = null
    var chromaUniform = ChromaUniform()

    fun computeChromaBufferSlice() {
        // Set custom chroma uniforms
        val chromaSize = 30f * (client.window.guiScaledWidth / 100f)
        val ticks = TickScheduler.Client.currentTick.toFloat()
        val chromaSpeed = 360f / 360f
        val timeOffset = ticks * chromaSpeed
        val saturation = 0.75f

        chromaBufferSlice = chromaUniform.writeWith(chromaSize, timeOffset, saturation)
    }

    // This 'should' be fine being injected into GuiRenderer's render pass since if the bound pipeline's shader doesn't
    // have a uniform with the given name, then the buffer slice will never be bound
    fun insertChromaSetUniform(renderPass: RenderPass) {
        // A very explicit name is given since the uniform will show up in RenderPassImpl's simpleUniforms
        // map, and so it is made clear where this uniform is from

        chromaBufferSlice?.let { renderPass.setUniform("ChromaUniforms", it) }
    }
}