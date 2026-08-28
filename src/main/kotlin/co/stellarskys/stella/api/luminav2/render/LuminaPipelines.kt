package co.stellarskys.stella.api.luminav2.render

import co.stellarskys.stella.Stella
import com.mojang.blaze3d.GpuFormat
import com.mojang.blaze3d.PrimitiveTopology
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.BlendFactor
import com.mojang.blaze3d.platform.BlendOp
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.client.renderer.BindGroupLayouts
import net.minecraft.resources.Identifier
import java.util.Optional

object LuminaPipelines {
    private val OVERWRITE = BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendOp.ADD)
    private val MUL_DST_ALPHA = BlendFunction(BlendFactor.DST_ALPHA, BlendFactor.ZERO, BlendOp.ADD)

    private val LUMINA_SNIPPET: RenderPipeline.Snippet = RenderPipeline.builder()
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        .withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        .withCull(false)
        .buildSnippet()

    private val SHAPE_SNIPPET: RenderPipeline.Snippet = RenderPipeline.builder(LUMINA_SNIPPET)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        .withVertexShader(id("lumina_shape"))
        .withFragmentShader(id("lumina_shape"))
        .buildSnippet()

    private val TEXTURE_SNIPPET: RenderPipeline.Snippet = RenderPipeline.builder(LUMINA_SNIPPET)
        .withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        .withVertexShader(id("lumina_tex"))
        .withFragmentShader(id("lumina_tex"))
        .buildSnippet()

    val SHAPE: RenderPipeline = RenderPipeline.builder(SHAPE_SNIPPET)
        .withLocation(id("lumina/lumina_shape"))
        .build()

    val TEXTURE: RenderPipeline = RenderPipeline.builder(TEXTURE_SNIPPET)
        .withLocation(id("lumina/lumina_texture"))
        .build()

    val SHAPE_WRITE: RenderPipeline = RenderPipeline.builder(SHAPE_SNIPPET)
        .withLocation(id("lumina/lumina_shape_write"))
        .withColorTargetState(setAlpha(OVERWRITE))
        .build()

    val TEXTURE_WRITE: RenderPipeline = RenderPipeline.builder(TEXTURE_SNIPPET)
        .withLocation(id("lumina/lumina_texture_write"))
        .withColorTargetState(setAlpha(OVERWRITE))
        .build()

    val SHAPE_IN: RenderPipeline = RenderPipeline.builder(SHAPE_SNIPPET)
        .withLocation(id("lumina/lumina_shape_in"))
        .withColorTargetState(ColorTargetState(MUL_DST_ALPHA))
        .build()

    val TEXTURE_IN: RenderPipeline = RenderPipeline.builder(TEXTURE_SNIPPET)
        .withLocation(id("lumina/lumina_texture_in"))
        .withColorTargetState(ColorTargetState(MUL_DST_ALPHA))
        .build()

    private fun id(location: String): Identifier = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, location)
    private fun setAlpha(blend: BlendFunction) = ColorTargetState(Optional.of(blend), GpuFormat.RGBA8_UNORM, ColorTargetState.WRITE_ALPHA)
}