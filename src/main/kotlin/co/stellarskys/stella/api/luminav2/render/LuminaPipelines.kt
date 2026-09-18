package co.stellarskys.stella.api.luminav2.render

import co.stellarskys.stella.Stella
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.systems.RenderPass
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import net.minecraft.resources.Identifier
import java.util.Optional

//? if > 26.1 {
  /*import com.mojang.blaze3d.GpuFormat
  import com.mojang.blaze3d.PrimitiveTopology
  import com.mojang.blaze3d.platform.BlendFactor
  import com.mojang.blaze3d.platform.BlendOp
  import net.minecraft.client.renderer.BindGroupLayouts
*///? } else {
import com.mojang.blaze3d.platform.DestFactor
import com.mojang.blaze3d.platform.SourceFactor
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.VertexFormat
 //? }

object LuminaPipelines {
    //? if > 26.1 {
    /*private val OVERWRITE = BlendFunction(BlendFactor.ONE, BlendFactor.ZERO, BlendOp.ADD)
    private val MUL_DST_ALPHA = BlendFunction(BlendFactor.DST_ALPHA, BlendFactor.ZERO, BlendOp.ADD)
    *///? } else {
    private val OVERWRITE = BlendFunction(SourceFactor.ONE, DestFactor.ZERO)
    private val MUL_DST_ALPHA = BlendFunction(SourceFactor.DST_ALPHA, DestFactor.ZERO)
    //? }


    private val LUMINA_SNIPPET: RenderPipeline.Snippet = RenderPipeline.builder()
        .withColorTargetState(ColorTargetState(BlendFunction.TRANSLUCENT))
        //? if > 26.1 {
        /*.withPrimitiveTopology(PrimitiveTopology.TRIANGLES)
        .withBindGroupLayout(BindGroupLayouts.PROJECTION)
        *///? } else {
        .withUniform("Projection", UniformType.UNIFORM_BUFFER)
        //? }
        .withCull(false)
        .buildSnippet()

    private val SHAPE_SNIPPET: RenderPipeline.Snippet = RenderPipeline.builder(LUMINA_SNIPPET)
        //? if > 26.1 {
        /*.withVertexBinding(0, DefaultVertexFormat.POSITION_COLOR)
        *///? } else {
        .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.TRIANGLES)
        //? }
        .withVertexShader(id("lumina_shape"))
        .withFragmentShader(id("lumina_shape"))
        .buildSnippet()

    private val TEXTURE_SNIPPET: RenderPipeline.Snippet = RenderPipeline.builder(LUMINA_SNIPPET)
        //? if > 26.1 {
        /*.withVertexBinding(0, DefaultVertexFormat.POSITION_TEX_COLOR)
        .withBindGroupLayout(BindGroupLayouts.SAMPLER0)
        *///? } else {
        .withVertexFormat(DefaultVertexFormat.POSITION_TEX_COLOR, VertexFormat.Mode.TRIANGLES)
        .withSampler("Sampler0")
        //? }
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
    private fun setAlpha(blend: BlendFunction) = ColorTargetState(Optional.of(blend), /*? if > 26.1 { */ /*GpuFormat.RGBA8_UNORM, *//*? }*/ ColorTargetState.WRITE_ALPHA)

    fun RenderPass.usePipeline(pipeline: RenderPipeline) =
        setPipeline(/*? if >= 26.3 {*/ /*RenderSystem.getCompiledPipeline(pipeline) *//*?} else {*/ pipeline /*?}*/)
}