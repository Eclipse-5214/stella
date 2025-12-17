package co.stellarskys.stella.utils.render.vexel

import co.stellarskys.vexel.core.VexelWindow
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniKey
import dev.deftu.omnicore.api.client.input.OmniKeys
import dev.deftu.omnicore.api.client.input.OmniMouse
import dev.deftu.omnicore.api.client.input.OmniMouseButton
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.render.OmniResolution
import dev.deftu.omnicore.api.client.screen.KeyPressEvent
import dev.deftu.omnicore.api.client.screen.OmniScreen
import dev.deftu.omnicore.api.client.screen.closeScreen
import dev.deftu.textile.Text

abstract class NVGScreen(screenName: String = "NVG-Screen") : OmniScreen(Text.literal(screenName)) {
    private var lastX: Double = -1.0
    private var lastY: Double = -1.0

    val window = VexelWindow()

    override fun onScreenClose() {
        window.cleanup()
        super.onScreenClose()
    }

    override fun onResize(width: Int, height: Int) {
        window.onWindowResize()
        super.onResize(width, height)
    }

    override fun onMouseClick(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        return window.mouseClick(button.code)
    }

    override fun onMouseRelease(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        return window.mouseRelease(button.code)
    }

    override fun onMouseScroll(x: Double, y: Double, amount: Double, horizontalAmount: Double): Boolean {
        window.mouseScroll(horizontalAmount, amount)
        return super.onMouseScroll(x, y, amount, horizontalAmount)
    }

    override fun onKeyPress(
        key: OmniKey,
        scanCode: Int,
        typedChar: Char,
        modifiers: KeyboardModifiers,
        event: KeyPressEvent
    ): Boolean {
        val handled = window.charType(key.code, scanCode, typedChar)
        if (!handled && key.code == OmniKeys.KEY_ESCAPE.code) closeScreen()
        return handled
    }

    /**
     * Called after the elements and animations render.
     */
    open fun onRenderGui(ctx: OmniRenderingContext) {}

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        val newX = OmniMouse.rawX
        val newY = OmniMouse.rawY
        if (newX != lastX || newY != lastY) {
            window.mouseMove()
            lastX = newX
            lastY = newY
        }

        super.onRender(ctx, mouseX, mouseY, tickDelta)

        val context = ctx.graphics ?: return
        NVGSpecialRenderer.draw(context, 0, 0, OmniResolution.windowWidth, OmniResolution.windowHeight) {
            window.draw()
        }

        onRenderGui(ctx)
    }
}