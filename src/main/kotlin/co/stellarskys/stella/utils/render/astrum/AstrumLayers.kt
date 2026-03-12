package co.stellarskys.stella.utils.render.astrum

import net.minecraft.client.renderer.rendertype.LayeringTransform
import net.minecraft.client.renderer.rendertype.OutputTarget
import net.minecraft.client.renderer.rendertype.RenderSetup
import net.minecraft.client.renderer.rendertype.RenderType

object AstrumLayers {
    private val LINES_THROUGH_WALLS = RenderType.create(
        "lines_through_walls",
        RenderSetup.builder(AstrumPipelines.LINES_THROUGH_WALLS)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup()
    )

    private val LINES = RenderType.create(
        "lines",
        RenderSetup.builder(AstrumPipelines.LINES)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .setOutputTarget(OutputTarget.ITEM_ENTITY_TARGET)
            .createRenderSetup()
    )


    val FILLED: RenderType = RenderType.create(
        "filled",
        RenderSetup.builder(AstrumPipelines.FILLED)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .sortOnUpload()
            .createRenderSetup()
    )


    val FILLED_THROUGH_WALLS: RenderType = RenderType.create(
        "filled_through_walls",
        RenderSetup.builder(AstrumPipelines.FILLED_THROUGH_WALLS)
            .sortOnUpload()
            .createRenderSetup()
    )

    /*
    fun createLineLayer(width: Double, depth: Boolean): RenderType {
       return if (depth) LINES else LINES_THROUGH_WALLS
    }
    */

    fun getLines(width: Double, depth: Boolean): RenderType = if (depth) LINES else LINES_THROUGH_WALLS
    fun getFilled(depth: Boolean): RenderType = if (depth) FILLED else FILLED_THROUGH_WALLS
}