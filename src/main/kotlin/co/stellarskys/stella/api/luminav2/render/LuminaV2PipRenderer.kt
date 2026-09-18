package co.stellarskys.stella.api.luminav2.render

import co.stellarskys.stella.api.luminav2.LuminaV2
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.mixins.accessors.AccessorPictureInPictureRenderer
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.renderer.SubmitNodeCollector
import net.minecraft.client.renderer.state.gui.pip.PictureInPictureRenderState
import org.joml.Matrix3x2f
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.Optional
import java.util.OptionalInt
import kotlin.math.roundToInt

//? if < 26.2 {
import net.minecraft.client.renderer.MultiBufferSource
//? }

//? if > 26.1 {
 /*class LuminaV2PipRenderer: PictureInPictureRenderer<LuminaV2PipRenderer.LuminaRenderState>() {
*///? } else {
class LuminaV2PipRenderer(bufferSource: MultiBufferSource.BufferSource): PictureInPictureRenderer<LuminaV2PipRenderer.LuminaRenderState>(bufferSource) {
//? }
    override fun renderToTexture(renderState: LuminaRenderState, poseStack: PoseStack /*? if > 26.1 {*//*, submitNodeCollector: SubmitNodeCollector *//*?}*/) {
        @Suppress("CAST_NEVER_SUCCEEDS")
        val colorView = (this as AccessorPictureInPictureRenderer).texterView!!
        val device = RenderSystem.getDevice()
        val entries = renderState.entries
        val transform = RenderSystem.getDynamicUniforms().writeTransform(
            Matrix4f(),
            //? if < 26.2 {
            Vector4f(1f, 1f, 1f, 1f),
            Vector3f(),
            Matrix4f()
            //? }
        )

        entries.forEach {
            when(it) {
                is LuminaTextureRenderer.ImageEntry -> it.image.upload()
                is LuminaTextureRenderer.TextEntry -> it.font.atlas.upload()
            }
        }

        device.createCommandEncoder().createRenderPass({ "Lumina v2" }, colorView, /*? if > 26.1 {*//*Optional.empty()*//*?} else {*/OptionalInt.empty()/*?}*/).use { pass ->
            RenderSystem.bindDefaultUniforms(pass)
            pass.setUniform("DynamicTransforms", transform)

            var i = 0
            while (i < entries.size) {
                val start = i
                val type = entries[i]::class
                val scissor = entries[i].scissor
                // TODO Each mask should get its own PIP
                val mask = entries[i].mask
                while (i < entries.size && entries[i]::class == type && entries[i].scissor == scissor && entries[i].mask == mask) i++
                val batch = entries.subList(start, i)

                if(!setScissor(pass, scissor, LuminaV2.dpr)) continue

                @Suppress("UNCHECKED_CAST")
                when (batch.first()) {
                    is LuminaShapeRenderer.ShapeEntry -> LuminaShapeRenderer.drawShapes(device, pass, batch as List<LuminaShapeRenderer.ShapeEntry>)
                    is LuminaTextureRenderer.TextEntry -> LuminaTextureRenderer.drawText(device, pass, batch as List<LuminaTextureRenderer.TextEntry>)
                    is LuminaTextureRenderer.ImageEntry -> LuminaTextureRenderer.drawImages(device, pass, batch as List<LuminaTextureRenderer.ImageEntry>)
                }
            }
        }
    }

    override fun getRenderStateClass(): Class<LuminaRenderState> = LuminaRenderState::class.java
    override fun getTextureLabel(): String = "lumina_renderer"

    data class LuminaRenderState(
        private val width: Int,
        private val height: Int,
        private val scissor: ScreenRectangle?,
        private val bounds: ScreenRectangle?,
        val entries: List<LuminaV2.Entry>
    ) : PictureInPictureRenderState {
        override fun scale(): Float = 1f
        override fun x0(): Int = 0
        override fun y0(): Int = 0
        override fun x1(): Int = width
        override fun y1(): Int = height
        override fun scissorArea(): ScreenRectangle? = scissor
        override fun bounds(): ScreenRectangle? = bounds
    }

    private fun setScissor(pass: RenderPass, scissor: LuminaV2.ScissorRect?, dpr: Float): Boolean {
        if (scissor == null) { pass.disableScissor(); return true }

        val ww = Zenith.Res.viewportWidth
        val wh = Zenith.Res.viewportHeight

        fun clamp(v: Float, max: Int) = (v * dpr).roundToInt().coerceIn(0, max)

        val x = clamp(scissor.x, ww)
        val y = clamp(scissor.y, wh)
        val w = clamp(scissor.x + scissor.w, ww) - x
        val h = clamp(scissor.y + scissor.h, wh) - y

        if (w <= 0 || h <=0) return false

        pass.enableScissor(x, wh - y - h, w, h)
        return true
    }

    companion object {
        fun draw(context: GuiGraphicsExtractor, entries: List<LuminaV2.Entry>) {
            val scissor = context.scissorStack.peek()
            val pose = Matrix3x2f(context.pose())
            val bounds = createBounds(0, 0, context.guiWidth(), context.guiHeight(), pose, scissor)
            val state = LuminaRenderState(context.guiWidth(), context.guiHeight(), scissor, bounds, entries)
            context.guiRenderState.addPicturesInPictureState(state)
        }

        private fun createBounds(x0: Int, y0: Int, x1: Int, y1: Int, pose: Matrix3x2f, scissorArea: ScreenRectangle?): ScreenRectangle? {
            val screenRect = ScreenRectangle(x0, y0, x1 - x0, y1 - y0).transformMaxBounds(pose)
            return if (scissorArea != null) scissorArea.intersection(screenRect) else screenRect
        }
    }
}