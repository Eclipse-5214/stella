package co.stellarskys.stella.utils.render

import com.mojang.blaze3d.buffers.GpuBufferSlice
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.MultiBufferSource

class BetterGuiRenderer(
    renderState: GuiRenderState,
    bufferSource: MultiBufferSource.BufferSource,
    list: List<PictureInPictureRenderer<*>>
): GuiRenderer(renderState, bufferSource, list) {
    override fun render(gpuBufferSlice: GpuBufferSlice) {
        super.render(gpuBufferSlice)
    }
    
    fun betterDraw(){
            }
}