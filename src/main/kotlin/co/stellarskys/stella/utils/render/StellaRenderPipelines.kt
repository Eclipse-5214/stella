package co.stellarskys.stella.utils.render

import co.stellarskys.stella.Stella
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.RenderPipeline
import com.mojang.blaze3d.platform.DepthTestFunction
import com.mojang.blaze3d.shaders.UniformType
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import dev.deftu.omnicore.api.client.render.DrawMode
import dev.deftu.omnicore.api.client.render.pipeline.IrisShaderType
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipeline
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipelineSnippets
import dev.deftu.omnicore.api.client.render.pipeline.OmniRenderPipelines
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.ResourceLocation

object StellaRenderPipelines {
    val LINES = buildPipeline(
        "lines",
        OmniRenderPipelineSnippets.LINES,
        IrisShaderType.LINES
    )

    val LINES_THROUGH_WALLS = buildPipeline(
        "lines_through_walls",
        OmniRenderPipelineSnippets.LINES,
        IrisShaderType.LINES,
        OmniRenderPipeline.DepthTest.DISABLED
    )

    val FILLED = buildPipeline(
        "filled",
        OmniRenderPipelineSnippets.builder(OmniRenderPipelineSnippets.POSITION_COLOR).setDrawMode(DrawMode.TRIANGLE_STRIP).build(),
        IrisShaderType.BASIC
    )

    val FILLED_THROUGH_WALLS = buildPipeline(
        "filled_through_walls",
        OmniRenderPipelineSnippets.builder(OmniRenderPipelineSnippets.POSITION_COLOR).setDrawMode(DrawMode.TRIANGLE_STRIP).build(),
        IrisShaderType.BASIC,
        OmniRenderPipeline.DepthTest.DISABLED
    )


    private fun buildPipeline(location: String, snippet: OmniRenderPipeline.Snippet, shader: IrisShaderType, depthTest: OmniRenderPipeline.DepthTest? = null, ): OmniRenderPipeline {
        val pipeline = OmniRenderPipelines.builderWithDefaultShader(location = id(location), snippets = arrayOf(snippet)).setCulling(false).setIrisType(shader)
        if (depthTest != null) pipeline.setDepthTest(depthTest)
        return pipeline.build()
    }

    private fun id(path: String) = ResourceLocation.fromNamespaceAndPath(Stella.NAMESPACE, path)
}