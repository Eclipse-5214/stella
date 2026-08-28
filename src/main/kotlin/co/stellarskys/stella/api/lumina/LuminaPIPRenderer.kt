package co.stellarskys.stella.api.lumina

/*
import co.stellarskys.stella.mixins.accessors.AccessorGpuDevice
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import com.mojang.blaze3d.opengl.GlDevice
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.joml.Matrix3x2f

/*
 * Adapted from NVGSpecialRenderer.kt in OdinFabric.
 *
 * OdinFabric: https://github.com/odtheking/OdinFabric
 * BSD 3-Clause License, Copyright (c) 2025, odtheking
 */


class LuminaPIPRenderer(bufferSource: MultiBufferSource.BufferSource) : PictureInPictureRenderer<LuminaPIPRenderer.LuminaRenderState>(bufferSource) {
    override fun renderToTexture(state: LuminaRenderState, poseStack: PoseStack) {
        val device = RenderSystem.getDevice() as? AccessorGpuDevice ?: return
        val colorTex = RenderSystem.outputColorTextureOverride ?: return
        val glDepthTex = (RenderSystem.outputDepthTextureOverride?.texture() as? GlTexture) ?: return
        val (width, height) = colorTex.let { it.getWidth(0) to it.getHeight(0) }
        val bufferManager = (device.backend as? GlDevice)?.directStateAccess() ?: return
        val fboId = (colorTex.texture() as? GlTexture)?.getFbo(bufferManager, glDepthTex) ?: return

        Lumina.backend.setupRenderTarget(fboId.toLong(), width, height)
        state.renderContent(width, height)
        Lumina.backend.resetAfterRender()
    }

    override fun getTranslateY(height: Int, windowScaleFactor: Int): Float = height / 2f
    override fun getRenderStateClass(): Class<LuminaRenderState> = LuminaRenderState::class.java
    override fun getTextureLabel(): String = "lumina_renderer"

    data class LuminaRenderState(
        private val x: Int,
        private val y: Int,
        private val width: Int,
        private val height: Int,
        private val scissor: ScreenRectangle?,
        private val bounds: ScreenRectangle?,
        val renderContent: (Int, Int) -> Unit
    ) : PictureInPictureRenderState {
        override fun scale(): Float = 1f
        override fun x0(): Int = x
        override fun y0(): Int = y
        override fun x1(): Int = x + width
        override fun y1(): Int = y + height
        override fun scissorArea(): ScreenRectangle? = scissor
        override fun bounds(): ScreenRectangle? = bounds
    }

    companion object {
        fun draw(context: GuiGraphicsExtractor, x: Int, y: Int, width: Int, height: Int, renderContent: (Int, Int) -> Unit) {
            val scissor = context.scissorStack.peek()
            val pose = Matrix3x2f(context.pose())
            val bounds = createBounds(x, y, x + width, y + height, pose, scissor)
            val state = LuminaRenderState(x, y, width, height, scissor, bounds, renderContent)
            context.guiRenderState.addPicturesInPictureState(state)
        }

        private fun createBounds(x0: Int, y0: Int, x1: Int, y1: Int, pose: Matrix3x2f, scissorArea: ScreenRectangle?): ScreenRectangle? {
            val screenRect = ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose)
            return if (scissorArea != null) scissorArea.intersection(screenRect) else screenRect
        }
    }
}
 */
