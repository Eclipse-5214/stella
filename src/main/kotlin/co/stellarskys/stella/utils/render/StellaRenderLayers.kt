package co.stellarskys.stella.utils.render


//import co.stellarskys.stella.utils.render.layers.ChromaRenderLayer
import co.stellarskys.stella.utils.render.layers.ChromaRenderType
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap
import net.minecraft.client.renderer.RenderPipelines
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.RenderType.CompositeState
import java.util.OptionalDouble
import java.util.function.DoubleFunction

object StellaRenderLayers {
    private val linesThroughWallsLayers: Double2ObjectMap<RenderType.CompositeRenderType> = Double2ObjectOpenHashMap()
    private val chromaLinesLayer: Double2ObjectMap<RenderType.CompositeRenderType> = Double2ObjectOpenHashMap()
    private val linesLayers: Double2ObjectMap<RenderType.CompositeRenderType> = Double2ObjectOpenHashMap()

    /*
    val CHROMA_STANDARD: MultiPhase = ChromaRenderLayer(
        "stella_standard_chroma",
        RenderLayer.CUTOUT_BUFFER_SIZE,
        false,
        false,
        StellaRenderPipelines.CHROMA_STANDARD,
        MultiPhaseParameters.builder().build(false)
    )

    private val CHROMA_TEXTURED: java.util.function.Function<Identifier, RenderLayer> = Util.memoize {
            texture ->
        ChromaRenderLayer(
            "text_chroma",
            RenderLayer.CUTOUT_BUFFER_SIZE,
            false,
            false,
            StellaRenderPipelines.CHROMA_TEXT,
            MultiPhaseParameters.builder()
                .texture(RenderPhase.Texture(texture, TriState.FALSE, false))
                .build(false)
        )
    }
    */

    val CHROMA_3D: RenderType.CompositeRenderType = ChromaRenderType(
        "standard_chroma",
        RenderType.TRANSIENT_BUFFER_SIZE,
        affectsCrumbling = false,
        sortOnUpload = true,
        renderPipeline = StellaRenderPipelines.CHROMA_3D,
        state = CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    )

    private val CHROMA_LINES = DoubleFunction { width ->
        ChromaRenderType(
            "chroma_lines",
            RenderType.TRANSIENT_BUFFER_SIZE, false, false,
            StellaRenderPipelines.CHROMA_LINES,
            CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(width)))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false)
        )
    }

    private val LINES_THROUGH_WALLS = DoubleFunction { width ->
        RenderType.create(
            "lines_through_walls",
            RenderType.TRANSIENT_BUFFER_SIZE, false, false,
            StellaRenderPipelines.LINES_THROUGH_WALLS,
            CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(width)))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false)
        )
    }

    private val LINES = DoubleFunction { width ->
        RenderType.create(
            "lines",
            RenderType.TRANSIENT_BUFFER_SIZE, false, false,
            RenderPipelines.LINES,
            CompositeState.builder()
                .setLineState(RenderStateShard.LineStateShard(OptionalDouble.of(width)))
                .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
                .createCompositeState(false)
        )
    }

    val FILLED: RenderType.CompositeRenderType = RenderType.create(
        "filled", RenderType.TRANSIENT_BUFFER_SIZE, false, true,
        RenderPipelines.DEBUG_FILLED_BOX,
        CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    )


    val FILLED_THROUGH_WALLS: RenderType.CompositeRenderType = RenderType.create(
        "filled_through_walls", RenderType.TRANSIENT_BUFFER_SIZE, false, true,
        StellaRenderPipelines.FILLED_THROUGH_WALLS,
        CompositeState.builder()
            .setLayeringState(RenderStateShard.VIEW_OFFSET_Z_LAYERING)
            .createCompositeState(false)
    )

    fun getLinesThroughWalls(width: Double): RenderType.CompositeRenderType =
        linesThroughWallsLayers.computeIfAbsent(width, LINES_THROUGH_WALLS)

    fun getLines(width: Double): RenderType.CompositeRenderType =
        linesLayers.computeIfAbsent(width, LINES)


    fun getChromaLines(width: Double): RenderType.CompositeRenderType =
        chromaLinesLayer.computeIfAbsent(width, CHROMA_LINES)

    /*
    fun getChromaTextured(identifier: Identifier) = CHROMA_TEXTURED.apply(identifier)
     */
}
