package co.stellarskys.stella.utils.render

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