package co.stellarskys.stella.utils.config.ui.base

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
    var width = 120f
    var height = 25f
    var visible = true
    var parent: BaseElement? = null

    open var isAnimating = false

    val absoluteX: Float get() = (parent?.absoluteX ?: 0f) + x
    val absoluteY: Float get() = (parent?.absoluteY ?: 0f) + y

    fun isAreaHovered(rx: Float, ry: Float, rw: Float, rh: Float, mx: Float = mouse.scaledX.toFloat(), my: Float = mouse.scaledY.toFloat()) =
        mx in (absoluteX + rx)..(absoluteX + rx + rw) && my in (absoluteY + ry)..(absoluteY + ry + rh)


    // Rendering
    abstract fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float)
    open fun setVisibility(value: Boolean) { visible = value }

    // Mouse input
    open fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean = false
    open fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {}

    // Keyboard input
    open fun charTyped(char: Char): Boolean = false
    open fun keyPressed(keyCode: Int): Boolean = false
}