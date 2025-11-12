package co.stellarskys.stella.utils.render


//import co.stellarskys.stella.utils.render.layers.ChromaRenderLayer
import it.unimi.dsi.fastutil.doubles.Double2ObjectMap
import it.unimi.dsi.fastutil.doubles.Double2ObjectOpenHashMap
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.render.RenderLayer
import net.minecraft.client.render.RenderLayer.MultiPhase
import net.minecraft.client.render.RenderLayer.MultiPhaseParameters
import net.minecraft.client.render.RenderPhase
import net.minecraft.client.render.block.entity.BeaconBlockEntityRenderer
import net.minecraft.util.Identifier
import net.minecraft.util.TriState
import net.minecraft.util.Util
import java.util.OptionalDouble
import java.util.function.DoubleFunction

object StellaRenderLayers {
    private val linesThroughWallsLayers: Double2ObjectMap<RenderLayer.MultiPhase> = Double2ObjectOpenHashMap()
    private val chromaLinesLayer: Double2ObjectMap<RenderLayer.MultiPhase> = Double2ObjectOpenHashMap()
    private val linesLayers: Double2ObjectMap<RenderLayer.MultiPhase> = Double2ObjectOpenHashMap()

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

    val CHROMA_3D: MultiPhase = ChromaRenderLayer(
        "standard_chroma",
        RenderLayer.DEFAULT_BUFFER_SIZE,
        false,
        true,
        StellaRenderPipelines.CHROMA_3D,
        MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )

    private val CHROMA_LINES = DoubleFunction { width ->
        ChromaRenderLayer(
            "chroma_lines",
            RenderLayer.DEFAULT_BUFFER_SIZE, false, false,
            StellaRenderPipelines.CHROMA_LINES,
            MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(width)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }
     */

    private val LINES_THROUGH_WALLS = DoubleFunction { width ->
        RenderLayer.of(
            "lines_through_walls",
            RenderLayer.DEFAULT_BUFFER_SIZE, false, false,
            StellaRenderPipelines.LINES_THROUGH_WALLS,
            MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(width)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    private val LINES = DoubleFunction { width ->
        RenderLayer.of(
            "lines",
            RenderLayer.DEFAULT_BUFFER_SIZE, false, false,
            RenderPipelines.LINES,
            MultiPhaseParameters.builder()
                .lineWidth(RenderPhase.LineWidth(OptionalDouble.of(width)))
                .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
                .build(false)
        )
    }

    val FILLED: RenderLayer.MultiPhase = RenderLayer.of(
        "filled", RenderLayer.DEFAULT_BUFFER_SIZE, false, true,
        RenderPipelines.DEBUG_FILLED_BOX,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )


    val FILLEDTHROUGHWALLS: RenderLayer.MultiPhase = RenderLayer.of(
        "filled_through_walls", RenderLayer.DEFAULT_BUFFER_SIZE, false, true,
        StellaRenderPipelines.FILLED_THROUGH_WALLS,
        RenderLayer.MultiPhaseParameters.builder()
            .layering(RenderPhase.VIEW_OFFSET_Z_LAYERING)
            .build(false)
    )

    fun getLinesThroughWalls(width: Double): RenderLayer.MultiPhase =
        linesThroughWallsLayers.computeIfAbsent(width, LINES_THROUGH_WALLS)

    fun getLines(width: Double): RenderLayer.MultiPhase =
        linesLayers.computeIfAbsent(width, LINES)

    /*
    fun getChromaLines(width: Double): RenderLayer.MultiPhase =
        chromaLinesLayer.computeIfAbsent(width, CHROMA_LINES)

    fun getChromaTextured(identifier: Identifier) = CHROMA_TEXTURED.apply(identifier)
     */
}
