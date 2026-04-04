package co.stellarskys.stella.api.nvg

import com.mojang.blaze3d.opengl.GlConst
import com.mojang.blaze3d.opengl.GlStateManager
import com.mojang.blaze3d.opengl.GlTexture
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer

import net.minecraft.client.renderer.MultiBufferSource

import org.joml.Matrix3x2f

//? if > 1.21.10 {
/*import org.lwjgl.opengl.GL33C
*///?}

//? if > 1.21.11 {
/*import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
*///? } else {
 import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
//? }

/*
 * Adapted from NVGSpecialRenderer.kt in OdinFabric
 * https://github.com/odtheking/OdinFabric
 *
 * BSD 3-Clause License
 * Copyright (c) 2025, odtheking
 * See full license at: https://opensource.org/licenses/BSD-3-Clause
 */
class NVGPIPRenderer(bufferSource: MultiBufferSource.BufferSource) : PictureInPictureRenderer<NVGPIPRenderer.NVGRenderState>(bufferSource) {
    override fun renderToTexture(state: NVGRenderState, poseStack: PoseStack) {
        /*
        val colorTex = RenderSystem.outputColorTextureOverride ?: return
        val bufferManager = (RenderSystem.getDevice() as? GlDevice)?.directStateAccess() ?: return
        val glDepthTex = (RenderSystem.outputDepthTextureOverride?.texture() as? GlTexture) ?: return

        val (width, height) = colorTex.let { it.getWidth(0) to it.getHeight(0) }
        (colorTex.texture() as? GlTexture)?.getFbo(bufferManager, glDepthTex)?.apply {
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, this)
            GlStateManager._viewport(0, 0, width, height)
        }

        //? if > 1.21.10 {
        GL33C.glBindSampler(0, 0)
        //?}
        */
        val colorTex = RenderSystem.outputColorTextureOverride?: return
        val depthTex = RenderSystem.outputDepthTextureOverride?: return
        //val colorId = (colorTex.texture() as? GlTexture)?.glId() ?: return
        //val depthId = (depthTex.texture() as? GlTexture)?.glId() ?: return
        val width = colorTex.getWidth(0)
        val height = colorTex.getHeight(0)

        val fbo = (colorTex.texture() as? GlTexture)?.getFbo(null, depthTex.texture() as GlTexture)

        if (fbo != null) {
            GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, fbo)
            GlStateManager._viewport(0, 0, width, height)
        }

        NVGRenderer.beginFrame(width.toFloat(), height.toFloat())
        state.renderContent()
        NVGRenderer.endFrame()


        GlStateManager._disableDepthTest()
        GlStateManager._disableCull()
        GlStateManager._enableBlend()
        GlStateManager._blendFuncSeparate(770, 771, 1, 0)
    }


    override fun getTranslateY(height: Int, windowScaleFactor: Int): Float = height / 2f
    override fun getRenderStateClass(): Class<NVGRenderState> = NVGRenderState::class.java
    override fun getTextureLabel(): String = "nvg_renderer"

    data class NVGRenderState(
        private val x: Int,
        private val y: Int,
        private val width: Int,
        private val height: Int,
        private val poseMatrix: Matrix3x2f,
        private val scissor: ScreenRectangle?,
        private val bounds: ScreenRectangle?,
        val renderContent: () -> Unit
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
        /**
         * Draw NVG content as a special GUI element.
         *
         * @param context The GuiGraphics to draw to
         * @param x The x position
         * @param y The y position
         * @param width The width of the rendering area
         * @param height The height of the rendering area
         * @param renderContent A lambda that draws the NVG content
         */
        fun draw(
            context: GuiGraphics,
            x: Int,
            y: Int,
            width: Int,
            height: Int,
            renderContent: () -> Unit
        ) {
            val scissor = context.scissorStack.peek()
            val pose = Matrix3x2f(context.pose())
            val bounds = createBounds(x, y, x + width, y + height, pose, scissor)

            val state = NVGRenderState(
                x, y, width, height,
                pose, scissor, bounds,
                renderContent
            )

            //? if > 1.21.11 {
            /*context.guiRenderState.addPicturesInPictureState(state)
            *///? } else {
            context.guiRenderState.submitPicturesInPictureState(state)
            //? }
        }

        private fun createBounds(x0: Int, y0: Int, x1: Int, y1: Int, pose: Matrix3x2f, scissorArea: ScreenRectangle?): ScreenRectangle? {
            val screenRect = ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose)
            return if (scissorArea != null) scissorArea.intersection(screenRect) else screenRect
        }
    }
}