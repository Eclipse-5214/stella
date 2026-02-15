package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.config.ui.ConfigUI
import co.stellarskys.stella.utils.render.nvg.NVGRenderer
import dev.deftu.omnicore.api.client.input.OmniMouse
import dev.deftu.omnicore.api.client.render.OmniResolution
import net.minecraft.client.gui.GuiGraphics

abstract class BaseElement {
    val nvg get() = NVGRenderer
    val rez get() = OmniResolution
    val mouse = OmniMouse

    var x = 0f
    var y = 0f
    var width = 240f
    var height = 50f
    var visible = true
    var parent: BaseElement? = null

    open val canReceiveInput: Boolean
        get() = visible && (parent?.canReceiveInput ?: true)

    open var isAnimating = false

    open val absoluteX: Float get() = (parent?.absoluteX ?: 0f) + x
    open val absoluteY: Float get() = (parent?.absoluteY ?: 0f) + y

    open fun isAreaHovered(rx: Float, ry: Float, rw: Float, rh: Float, mx: Float = mouse.rawX.toFloat() / ConfigUI.UI_SCALE, my: Float = mouse.rawY.toFloat()  / ConfigUI.UI_SCALE) =
        mx in (absoluteX + rx)..(absoluteX + rx + rw) && my in (absoluteY + ry)..(absoluteY + ry + rh)


    // Rendering
    abstract fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float)
    open fun setVisibility(value: Boolean) {
        if (visible == value) return
        visible = value
    }

    // Mouse input
    open fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float, horizontalAmount: Float): Boolean = false
    open fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean = false
    open fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {}

    // Keyboard input
    open fun charTyped(char: Char, modifiers: Int): Boolean = false
    open fun keyPressed(keyCode: Int, modifiers: Int): Boolean = false
}