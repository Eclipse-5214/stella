package co.stellarskys.stella.utils.render

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import dev.deftu.omnicore.api.client.client
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.pip.*
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.fog.FogRenderer

//#if MC > 1.21.9
//$$ import net.minecraft.client.renderer.SubmitNodeStorage
//$$ import net.minecraft.client.renderer.feature.FeatureRenderDispatcher
//#endif

/**
 * A custom GUI rendering pipeline that mimics the behavior of the main GameRenderer.
 * This allows for manual flushing of render batches, enabling the injection of custom
 * rendering calls (like NanoVG) between vanilla draw calls.
 *
 * It runs completely independently of the game's main GuiRenderer.
 */
object CustomGuiRenderer {
    private val minecraft: Minecraft = client
    private val guiRenderState: GuiRenderState = GuiRenderState()
    private val renderBuffers = minecraft.renderBuffers()
    private val bufferSource = renderBuffers.bufferSource()

    //#if MC > 1.21.9
    //$$ private val atlasManager = minecraft.atlasManager
    //$$ private val submitNodeStorage = SubmitNodeStorage()
    //$$ private val featureRenderDispatcher = FeatureRenderDispatcher(submitNodeStorage, minecraft.blockRenderer, bufferSource, atlasManager, renderBuffers.outlineBufferSource(), renderBuffers.crumblingBufferSource(), minecraft.font)
    //#endif

    private val pipRenderers = listOf(
        GuiEntityRenderer(bufferSource, minecraft.entityRenderDispatcher),
        GuiSkinRenderer(bufferSource),
        GuiBookModelRenderer(bufferSource),
        GuiBannerResultRenderer(
            bufferSource,

            //#if MC > 1.21.9
            //$$ atlasManager
            //#endif
        ),
        GuiSignRenderer(
            bufferSource,

            //#if MC > 1.21.9
            //$$ atlasManager
            //#endif
        ),
        GuiProfilerChartRenderer(bufferSource)
    )

    private val guiRenderer: GuiRenderer = GuiRenderer(
        guiRenderState,
        bufferSource,

        //#if MC > 1.21.9
        //$$ submitNodeStorage,
        //$$ featureRenderDispatcher,
        //#endif

        pipRenderers
    )

    private val fogRenderer: FogRenderer = FogRenderer()

    init { EventBus.register<GameEvent.Stop> { close() } }

    /**
     * Executes a full rendering pass with support for layering.
     *
     * @param main A lambda to render it
     */
    fun render(main: (GuiGraphics) -> Unit ) {
        guiRenderState.reset()
        val graphics = GuiGraphics(minecraft, guiRenderState)
        main(graphics)
        guiRenderer.render(fogRenderer.getBuffer(FogRenderer.FogMode.NONE))
        guiRenderer.incrementFrameNumber()
    }

    /**
     * Cleans up the resources used by the renderer. Should be called on game exit.
     */
    fun close() {
        guiRenderer.close()
        fogRenderer.close()

        //#if MC > 1.21.9
        //$$ featureRenderDispatcher.close()
        //#endif
    }
}