package co.stellarskys.stella.utils.config

import co.stellarskys.stella.Stella
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.render.URenderPipeline
import gg.essential.universal.vertex.UBufferBuilder
import gg.essential.universal.shader.BlendState
import net.minecraft.client.texture.GlTexture
import net.minecraft.util.Identifier
import java.awt.Color

fun drawTexture(
    matrices: UMatrixStack,
    pipeline: URenderPipeline,
    sprite: Identifier,
    x: Double,
    y: Double,
    drawWidth: Double,
    drawHeight: Double,
    u: Double = 0.0,
    v: Double = 0.0,
    regionWidth: Double = drawWidth,
    regionHeight: Double = drawHeight,
    textureWidth: Double = 64.0,
    textureHeight: Double = 64.0,
    color: Color = Color.WHITE
) {
    val buffer = UBufferBuilder.create(UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR)
    UGraphics.bindTexture(0, sprite)
    val texture = Stella.mc.textureManager.getTexture(sprite)
    texture.setFilter(false, false)
    val glTexture = texture.glTexture as GlTexture

    val x2 = x + drawWidth
    val y2 = y + drawHeight

    val u1 = u / textureWidth
    val u2 = (u + regionWidth) / textureWidth
    val v1 = v / textureHeight
    val v2 = (v + regionHeight) / textureHeight

    buffer.pos(matrices, x, y, 0.0).tex(u1, v1).color(color).endVertex()
    buffer.pos(matrices, x, y2, 0.0).tex(u1, v2).color(color).endVertex()
    buffer.pos(matrices, x2, y2, 0.0).tex(u2, v2).color(color).endVertex()
    buffer.pos(matrices, x2, y, 0.0).tex(u2, v1).color(color).endVertex()

    buffer.build()?.drawAndClose(pipeline) {
        texture(0, glTexture.glId)
    }
}

object UCRenderPipelines {
    private val translucentBlendState = BlendState(BlendState.Equation.ADD, BlendState.Param.SRC_ALPHA, BlendState.Param.ONE_MINUS_SRC_ALPHA, BlendState.Param.ONE)

    val guiPipeline = URenderPipeline.builderWithDefaultShader("stella:pipeline/gui",
        UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_COLOR
    ).apply {
        blendState = translucentBlendState
        depthTest = URenderPipeline.DepthTest.LessOrEqual
    }.build()

    val guiTexturePipeline = URenderPipeline.builderWithDefaultShader("stella:pipeline/gui_texture",
        UGraphics.DrawMode.QUADS, UGraphics.CommonVertexFormats.POSITION_TEXTURE_COLOR
    ).apply {
        blendState = translucentBlendState
        depthTest = URenderPipeline.DepthTest.LessOrEqual
    }.build()
}