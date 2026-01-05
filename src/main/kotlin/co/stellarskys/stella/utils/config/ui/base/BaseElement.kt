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
    var width = 0f
    var height = 0f

    open val absoluteX get() = x
    open val absoluteY get() = y

    fun isAreaHovered(mouseX: Float, mouseY: Float): Boolean =
        mouseX >= absoluteX && mouseX <= absoluteX + width && mouseY >= absoluteY && mouseY <= absoluteY + height

    // Rendering
    abstract fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float)

    // Mouse input
    open fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean = false
    open fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {}

    // Keyboard input
    open fun charTyped(char: Char): Boolean = false
    open fun keyPressed(keyCode: Int): Boolean = false
}