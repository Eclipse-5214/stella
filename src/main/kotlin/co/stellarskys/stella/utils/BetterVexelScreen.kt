package co.stellarskys.stella.utils

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GuiEvent
import net.minecraft.client.gui.GuiGraphics
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.events.EventCall
import xyz.meowing.knit.api.input.KnitKeys
import xyz.meowing.knit.api.render.KnitResolution
import xyz.meowing.knit.api.screen.KnitScreen
import xyz.meowing.vexel.core.VexelWindow
import xyz.meowing.vexel.utils.render.NVGRenderer
import java.util.Timer
import kotlin.concurrent.schedule

abstract class BetterVexelScreen(screenName: String = "Vexel-Screen"): KnitScreen(screenName) {
    var renderEvent: EventCall? = null

    var initialized = false
        private set
    var hasInitialized = false
        private set

    val window = VexelWindow()

    open fun afterInitialization() {}

    final override fun onInitGui() {
        if (!hasInitialized) {
            hasInitialized = true
            initialized = true

            NVGRenderer.cleanCache()

            afterInitialization()

            renderEvent = EventBus.register<GuiEvent.RenderHUD> {
                if (KnitClient.client.screen == this) {
                    NVGRenderer.beginFrame(KnitResolution.windowWidth.toFloat(), KnitResolution.windowHeight.toFloat())
                    window.draw()
                    NVGRenderer.endFrame()

                    onRenderGui(it.context)
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
    open fun onRenderGui(context: GuiGraphics) {}

    fun display() {
        Timer().schedule(50) {
            KnitClient.client.execute {
                KnitClient.client.setScreen(this@BetterVexelScreen)
            }
        }
    }
}