package co.stellarskys.stella.api.horizon.mc

import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphics

abstract class BaseElement {
    val ren2d get() = Render2D
    val rez get() = Zenith.Res
    val mouse = Zenith.Mouse

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

    open fun isAreaHovered(rx: Float, ry: Float, rw: Float, rh: Float, mx: Float = mouse.scaledX.toFloat(), my: Float = mouse.scaledY.toFloat()) =
        mx in (absoluteX + rx)..(absoluteX + rx + rw) && my in (absoluteY + ry)..(absoluteY + ry + rh)

    open fun isTextHovered(text: String, rx: Float, ry: Float, scale: Float = 1f, mx: Float = mouse.scaledX.toFloat(), my: Float = mouse.scaledY.toFloat()) =
        isAreaHovered(rx, ry, width, text.width().toFloat() * scale, mx, my)

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
    open fun charTyped(char: Char): Boolean = false
    open fun keyPressed(keyCode: Int, modifiers: Int): Boolean = false

}

fun <T : BaseElement> T.addTo(parent: ParentElement): T { this.parent = parent; parent.elements.add(this); return this }