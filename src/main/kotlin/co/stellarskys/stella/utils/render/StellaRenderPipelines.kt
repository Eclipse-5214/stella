package co.stellarskys.stella.utils.render

//#if MC >= 1.21.5

import co.stellarskys.stella.Stella
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gl.UniformType
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier

object StellaRenderPipelines {
    val CHROMA_STANDARD: RenderPipeline = RenderPipelines.register(RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
        .withLocation(Identifier.of(Stella.NAMESPACE, "chroma_standard"))
        .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withVertexShader(ShaderPath("standard_chroma"))
        .withFragmentShader(ShaderPath("standard_chroma"))
        .withUniform("chromaSize",UniformType.FLOAT)
        .withUniform("timeOffset", UniformType.FLOAT)
        .withUniform("saturation", UniformType.FLOAT)
        .withUniform("forwardDirection", UniformType.INT)
        .build()
    )

    val CHROMA_TEXT: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.MATRICES_SNIPPET)
            .withLocation(Identifier.of(Stella.NAMESPACE, "chroma_text"))
            .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexShader(ShaderPath("textured_chroma"))
            .withFragmentShader(ShaderPath("textured_chroma"))
            .withSampler("Sampler0")
            .withUniform("chromaSize",UniformType.FLOAT)
            .withUniform("timeOffset", UniformType.FLOAT)
            .withUniform("saturation", UniformType.FLOAT)
            .withUniform("forwardDirection", UniformType.INT)
            .build()
    )

    val CHROMA_3D: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(Stella.NAMESPACE, "chroma_3d"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withBlend(BlendFunction.TRANSLUCENT)
            .withVertexShader(ShaderPath("3d_chroma"))
            .withFragmentShader(ShaderPath("3d_chroma"))
            .withUniform("chromaSize",UniformType.FLOAT)
            .withUniform("timeOffset", UniformType.FLOAT)
            .withUniform("saturation", UniformType.FLOAT)
            .withUniform("forwardDirection", UniformType.INT)
            .build()
    )

    val CHROMA_LINES: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(Stella.NAMESPACE, "chroma_lines"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
            .withVertexShader(ShaderPath("3d_chroma"))
            .withFragmentShader(ShaderPath("3d_chroma"))
            .withUniform("chromaSize",UniformType.FLOAT)
            .withUniform("timeOffset", UniformType.FLOAT)
            .withUniform("saturation", UniformType.FLOAT)
            .withUniform("forwardDirection", UniformType.INT)
            .build()
    )

    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.RENDERTYPE_LINES_SNIPPET)
            .withLocation(Identifier.of(Stella.NAMESPACE, "lines_through_walls"))
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val FILLED_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
            .withLocation(Identifier.of(Stella.NAMESPACE, "filled_through_walls"))
            .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_STRIP)
            .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
            .build()
    )

    val BEACON_BEAM_OPAQUE = RenderPipeline.builder(RenderPipelines.RENDERTYPE_BEACON_BEAM_SNIPPET)
        .withLocation("stella/beacon_beam_opaque")
        .build()

    val BEACON_BEAM_OPAQUE_THROUGH_WALLS = RenderPipeline.builder(RenderPipelines.RENDERTYPE_BEACON_BEAM_SNIPPET)
        .withLocation("stella/beacon_beam_opaque_through_walls")
        .withDepthWrite(false)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()

    val BEACON_BEAM_TRANSLUCENT = RenderPipeline.builder(RenderPipelines.RENDERTYPE_BEACON_BEAM_SNIPPET)
        .withLocation("stella/beacon_beam_translucent")
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .build()

    val BEACON_BEAM_TRANSLUCENT_THROUGH_WALLS = RenderPipeline.builder(RenderPipelines.RENDERTYPE_BEACON_BEAM_SNIPPET)
        .withLocation("stella/beacon_beam_translucent_through_walls")
        .withDepthWrite(false)
        .withBlend(BlendFunction.TRANSLUCENT)
        .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .build()

    fun ShaderPath(SNAME: String) = Identifier.of(Stella.NAMESPACE, SNAME)
}

//#endif