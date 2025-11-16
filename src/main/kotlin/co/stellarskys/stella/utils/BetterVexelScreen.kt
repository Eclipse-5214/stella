package co.stellarskys.stella.utils

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GuiEvent
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.render.state.GuiRenderState
import net.minecraft.client.renderer.fog.FogRenderer
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.events.EventCall
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.knit.api.screen.KnitScreen
import xyz.meowing.vexel.core.VexelWindow
import xyz.meowing.vexel.utils.render.NVGRenderer
import java.awt.Color

abstract class BetterVexelScreen(screenName: String = "Vexel-Screen"): KnitScreen(screenName) {
    var renderEvent: EventCall? = null

    var initialized = false
        private set
    var hasInitialized = false
        private set

    val window = VexelWindow()

    val fog = FogRenderer()
    val state = GuiRenderState()
    val renderer = GuiRenderer(GuiRenderState(), KnitClient.client.renderBuffers().bufferSource(), listOf())

    open fun afterInitialization() {}

    final override fun onInitGui() {
        if (!hasInitialized) {
            hasInitialized = true
            initialized = true

            NVGRenderer.cleanCache()

            afterInitialization()

            renderEvent = EventBus.register<GuiEvent.NVG.Render> {
                if (KnitClient.client.screen == this) {
                    window.draw()

                    val graphics = GuiGraphics(KnitClient.client, state)

                    it.context.fill(0,0,width, height, Color.WHITE.rgb)

                    renderer.render(fog.getBuffer(FogRenderer.FogMode.NONE))
                    renderer.incrementFrameNumber()
                }
            }
        } else {
            initialized = true
        }
    }

    override fun onCloseGui() {
        window.cleanup()
        renderEvent?.unregister()
        renderEvent = null
        hasInitialized = false
    }

    override fun onResizeGui() {
        window.onWindowResize()
    }

    override fun onMouseClick(mouseX: Int, mouseY: Int, button: Int) {
        window.mouseClick(button)
    }

    override fun onMouseRelease(mouseX: Int, mouseY: Int, button: Int) {
        window.mouseRelease(button)
    }

    override fun onMouseMove(mouseX: Int, mouseY: Int) {
        window.mouseMove()
    }

    override fun onMouseScroll(horizontal: Double, vertical: Double) {
        window.mouseScroll(horizontal, vertical)
    }

    override fun onKeyType(typedChar: Char, keyCode: Int, scanCode: Int) {
        val handled = window.charType(keyCode, scanCode, typedChar)
        if (!handled && keyCode == KnitKeys.KEY_ESCAPE.code) close()
    }

    /**
     * Called after the elements and animations render.
     */
    open fun onRenderGui() {}
}