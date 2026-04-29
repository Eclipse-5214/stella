package co.stellarskys.stella.api.astrum

//? if > 1.21.10 {
 /*import net.minecraft.client.renderer.rendertype.LayeringTransform
   import net.minecraft.client.renderer.rendertype.OutputTarget
   import net.minecraft.client.renderer.rendertype.RenderSetup
   import net.minecraft.client.renderer.rendertype.RenderType
 *///? } else {
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import java.util.OptionalDouble
import java.util.concurrent.ConcurrentHashMap
//? }

object AstrumLayers {
    //? if <= 1.21.10 {
    private val linesCache = ConcurrentHashMap<Double, RenderType>()
    private val linesThroughWallsCache = ConcurrentHashMap<Double, RenderType>()
    //? }

    val FILLED: RenderType = RenderType.create(
        "filled",
        //? if > 1.21.10 {
         /*RenderSetup.builder(AstrumPipelines.FILLED)
            .setLayeringTransform(LayeringTransform.VIEW_OFFSET_Z_LAYERING)
            .sortOnUpload()
            .createRenderSetup()
         *///? } else {
        RenderType.TRANSIENT_BUFFER_SIZE, false, true,
        AstrumPipelines.FILLED,
        RenderType.CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
        //? }
    )


    val FILLED_THROUGH_WALLS: RenderType = RenderType.create(
        "filled_through_walls",
        //? if > 1.21.10 {
         /*RenderSetup.builder(AstrumPipelines.FILLED_THROUGH_WALLS)
            .sortOnUpload()
            .createRenderSetup()
         *///? } else {
        RenderType.TRANSIENT_BUFFER_SIZE, false, true,
        AstrumPipelines.FILLED_THROUGH_WALLS,
        RenderType.CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
        //? }
    )

    //? if > 1.21.10 {
     /*private val LINES_THROUGH_WALLS = RenderType.create(
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
    *///? } else {
    fun createLineLayer(width: Double, depth: Boolean): RenderType {
        val pipeLine = if (depth) AstrumPipelines.LINES else AstrumPipelines.LINES_THROUGH_WALLS
        return RenderType.create(
            "lines_${width}${if (depth) "" else "_through_walls"}",
            RenderType.TRANSIENT_BUFFER_SIZE, false, true,
            pipeLine,
            RenderType.CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(width)))
                .setLayeringState(if (depth) RenderStateShard.VIEW_OFFSET_Z_LAYERING else RenderStateShard.NO_LAYERING)
                .createCompositeState(false),
        )
    }
    //? }

    fun getLines(width: Double, depth: Boolean): RenderType =
        //? if > 1.21.10 {
         /*if (depth) LINES else LINES_THROUGH_WALLS
         *///? } else {
        (if (depth) linesCache else linesThroughWallsCache).computeIfAbsent(width) { createLineLayer(width, depth) }
        //? }

    fun getFilled(depth: Boolean): RenderType = if (depth) FILLED else FILLED_THROUGH_WALLS
}