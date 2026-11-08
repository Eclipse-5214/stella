package co.stellarskys.stella.api.astrum

import co.stellarskys.stella.Stella
import com.mojang.blaze3d.pipeline.BlendFunction
import com.mojang.blaze3d.pipeline.ColorTargetState
import com.mojang.blaze3d.pipeline.RenderPipeline
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.resources.Identifier
import java.util.Optional

//? if > 26.2 {
/*import net.minecraft.client.renderer.oit.OitPipelineSet
*///?}

object AstrumPipelines {
    val TRANSLUCENT = ColorTargetState(BlendFunction.TRANSLUCENT)

    val LINES: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id( "lines"))
            .withColorTargetState(TRANSLUCENT)
            .build()
    )

    val LINES_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
            .withLocation(id( "lines_through_walls"))
            .withColorTargetState(TRANSLUCENT)
            .withDepthStencilState(Optional.empty())
            .build()
    )

    val FILLED: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("filled"))
            .withColorTargetState(TRANSLUCENT)
            .build()
    )

    val FILLED_THROUGH_WALLS: RenderPipeline = RenderPipelines.register(
        RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
            .withLocation(id("filled_through_walls"))
            .withColorTargetState(TRANSLUCENT)
            .withDepthStencilState(Optional.empty())
            .build()
    )

    private fun id(path: String) = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, path)

    //? if > 26.2 {
    /*val OIT_LINES: OitPipelineSet = RenderPipelines.register(
        OitPipelineSet.builder("stella_lines", RenderPipeline.builder(RenderPipelines.OIT_LINES_SNIPPET))
            .build()
    )

    val OIT_LINES_THROUGH_WALLS: OitPipelineSet = RenderPipelines.register(
        OitPipelineSet.builder("stella_lines_through_walls", RenderPipeline.builder(RenderPipelines.OIT_LINES_SNIPPET))
            .withoutDepthTest()
            .build()
    )

    val OIT_FILLED: OitPipelineSet = RenderPipelines.register(
        OitPipelineSet.builder("stella_filled", RenderPipeline.builder(RenderPipelines.OIT_DEBUG_FILLED_SNIPPET))
            .build()
    )

    val OIT_FILLED_THROUGH_WALLS: OitPipelineSet = RenderPipelines.register(
        OitPipelineSet.builder("stella_filled_through_walls", RenderPipeline.builder(RenderPipelines.OIT_DEBUG_FILLED_SNIPPET))
            .withoutDepthTest()
            .build()
    )
    *///?}
}