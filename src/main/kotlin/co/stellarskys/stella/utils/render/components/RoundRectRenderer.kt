package co.stellarskys.stella.utils.render.components

import co.stellarskys.stella.utils.render.StellaRenderPipelines
import com.mojang.blaze3d.buffers.*
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.client.render.OmniResolution
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.navigation.ScreenRectangle
import net.minecraft.client.gui.render.pip.PictureInPictureRenderer
import net.minecraft.client.gui.render.state.pip.PictureInPictureRenderState
import net.minecraft.client.renderer.DynamicUniformStorage
import net.minecraft.client.renderer.MultiBufferSource
import org.joml.Vector3f
import org.joml.Vector4f
import java.awt.Color
import java.util.OptionalDouble
import java.util.OptionalInt
import kotlin.math.ceil
import kotlin.math.floor

// this is straight black magic
class RoundRectRenderer(buffers: MultiBufferSource.BufferSource) : PictureInPictureRenderer<RoundRectRenderer.RoundRectState>(buffers) {
    override fun getRenderStateClass() = RoundRectState::class.java
    override fun textureIsReadyToBlit(state: RoundRectState) = state == lastState
    override fun getTextureLabel(): String = "round_rect"
    private var lastState: RoundRectState? = null

    override fun renderToTexture(state: RoundRectState, poseStack: PoseStack) {
        val s = state.scale
        val tw = (state.extentX + 2 * RoundRectState.OUTSET + state.subpixelX) * s
        val th = (state.extentY + 2 * RoundRectState.OUTSET + state.subpixelY) * s

        val builder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION)
        builder.addVertex(0f, 0f, state.depth).addVertex(0f, th, state.depth)
            .addVertex(tw, th, state.depth).addVertex(tw, 0f, state.depth)
        val mesh = builder.buildOrThrow()

        val dynamicTransforms = RenderSystem.getDynamicUniforms().writeTransform(
            RenderSystem.getModelViewMatrix(), Vector4f(), Vector3f(),
            RenderSystem.getTextureMatrix(), RenderSystem.getShaderLineWidth()
        )

        val ubo = RoundRectUniforms.STORAGE.writeUniform { buf ->
            val crs = state.clampedRadius * s
            Std140Builder.intoBuffer(buf)
                .putVec4(state.subpixelX * s + (tw/2), state.subpixelY * s + (th/2), state.extentX * s, state.extentY * s)
                // Corrected: Pass all 4 components for u_Radii
                .putVec4(crs, crs, crs, crs)
                .putVec4(state.color).putVec4(state.color2 ?: state.color).putVec4(0f, 0f, 0f, 1f)
                .putVec2(0f, 1f).putFloat(state.edgeSoftness * s).putFloat(state.shadow * s)
        }

        val vBuf = StellaRenderPipelines.ROUND_RECT.vertexFormat.uploadImmediateVertexBuffer(mesh.vertexBuffer())
        val idx = RenderSystem.getSequentialBuffer(mesh.drawState().mode())

        mesh.use {
            val target = client.mainRenderTarget
            RenderSystem.getDevice().createCommandEncoder().createRenderPass(
                { "RoundRect" }, (RenderSystem.outputColorTextureOverride ?: target.colorTextureView) ?: return@use,
                OptionalInt.empty(), if (target.useDepth) RenderSystem.outputDepthTextureOverride ?: target.depthTextureView else null,
                OptionalDouble.empty()
            ).use { pass ->
                pass.setPipeline(StellaRenderPipelines.ROUND_RECT)
                RenderSystem.bindDefaultUniforms(pass)
                pass.setUniform("DynamicTransforms", dynamicTransforms)
                pass.setUniform("u", ubo)
                pass.setVertexBuffer(0, vBuf)
                pass.setIndexBuffer(idx.getBuffer(mesh.drawState().indexCount()), idx.type())
                pass.drawIndexed(0, 0, mesh.drawState().indexCount(), 1)
            }
        }
        lastState = state
    }

    companion object {
        fun draw(c: GuiGraphics, x: Float, y: Float, w: Float, h: Float, r: Float, col: Color, col2: Color? = null, s: Float = 0f, e: Float = 1f) {
            if (w <= 0.01f || h <= 0.01f) return
            val pose = c.pose()
            c.guiRenderState.submitPicturesInPictureState(RoundRectState(
                x = pose.m20() + (x * pose.m00()), y = pose.m21() + (y * pose.m00()),
                extentX = w, extentY = h, radius = r, color = col.toVec4(), color2 = col2?.toVec4(), shadow = s, edgeSoftness = e,
                scale = OmniResolution.scaleFactor.toFloat() * pose.m00(),
                scissorArea = c.scissorStack.peek()
            ))
        }
        private fun Color.toVec4() = Vector4f(red / 255f, green / 255f, blue / 255f, alpha / 255f)
    }

    data class RoundRectState(
        val x: Float, val y: Float, val extentX: Float, val extentY: Float, val radius: Float,
        val color: Vector4f, val color2: Vector4f?, val shadow: Float,
        val edgeSoftness: Float, val scale: Float, val scissorArea: ScreenRectangle?,
        val depth: Float = 0f
    ) : PictureInPictureRenderState {
        companion object { const val OUTSET = 14f }

        val subpixelX = x - floor(x)
        val subpixelY = y - floor(y)
        val clampedRadius = radius.coerceIn(0f, minOf(extentX, extentY) * 0.5f)
        private val relScale get() = scale / OmniResolution.scaleFactor.toFloat()

        override fun x0() = floor(x - (OUTSET * relScale)).toInt()
        override fun x1() = ceil(x + (extentX + OUTSET) * relScale).toInt()
        override fun y0() = floor(y - (OUTSET * relScale)).toInt()
        override fun y1() = ceil(y + (extentY + OUTSET) * relScale).toInt()

        override fun scale() = 1f
        override fun scissorArea() = scissorArea
        override fun bounds(): ScreenRectangle {
            val rect = ScreenRectangle(x0(), y0(), x1() - x0(), y1() - y0())
            return scissorArea?.intersection(rect) ?: rect
        }
    }

    object RoundRectUniforms { val STORAGE = DynamicUniformStorage<DynamicUniformStorage.DynamicUniform>("RoundRect UBO", 112, 4) }
}