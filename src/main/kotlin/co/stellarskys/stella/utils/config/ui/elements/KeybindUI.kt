package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config.core.Keybind
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import net.minecraft.client.gui.GuiGraphics
import org.lwjgl.glfw.GLFW
import java.awt.Color

class KeybindUI(initX: Float, initY: Float, val keybind: Keybind) : BaseElement() {
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var offset by offsetAnim

    private var isListening = false
    private val handler = keybind.value as Keybind.Handler

    init {
        x = initX; y = initY; width = 120f
        offset = if (visible) 0f else HEIGHT; height = HEIGHT - offset
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        offset = if (value) 0f else HEIGHT
        isAnimating = true
        if (!value) isListening = false // Stop listening if hidden
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return

        if (isAnimating) {
            height = (HEIGHT - offset).coerceAtLeast(0f)
            if (offsetAnim.done()) isAnimating = false
        }

        nvg.push(); nvg.translate(x, y); nvg.pushScissor(0f, 0f, width, height)

        // Background
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(keybind.name, 6f, 8f, 8f, Palette.Text.rgb, nvg.inter)

        // Keybind Box
        val boxW = 20f
        val boxH = 14f
        val boxX = width - boxW - 6f
        val boxY = (HEIGHT - boxH) / 2f

        val isHovered = isAreaHovered(boxX, boxY, boxW, boxH, mouseX, mouseY)

        // Render the button
        val bgColor = if (isListening) Palette.Purple.withAlpha(100).rgb else Palette.Base.rgb
        val borderColor = if (isListening || isHovered) Palette.Purple.rgb else Palette.Purple.withAlpha(80).rgb

        nvg.rect(boxX, boxY, boxW, boxH, bgColor, 3f)
        nvg.hollowRect(boxX, boxY, boxW, boxH, 1f, borderColor, 3f)

        // Key Text
        val displayStr = if (isListening) "..." else getKeyStringName(handler.keyCode())
        val textWidth = nvg.textWidth(displayStr, 7f, nvg.inter)
        nvg.text(displayStr, boxX + (boxW - textWidth) / 2f, boxY + 4f, 7f, Palette.Text.rgb, nvg.inter)

        nvg.popScissor(); nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible || offset > 1f || parent?.isAnimating == true) return false

        val boxW = 20f
        val boxH = 14f
        val boxX = width - boxW - 6f
        val boxY = (HEIGHT - boxH) / 2f

        if (isAreaHovered(boxX, boxY, boxW, boxH, mouseX, mouseY)) {
            isListening = !isListening
            return true
        }

        isListening = false // Clicked away
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, modifiers: Int): Boolean {
        if (!isListening) return false

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            handler.setCode(0) // Set to None
        } else {
            handler.setCode(keyCode)
        }

        isListening = false
        return true
    }

    private fun getKeyStringName(keyCode: Int): String = when (keyCode) {
        0 -> "None"
        340 -> "LShift"
        344 -> "RShift"
        341 -> "LCtrl"
        345 -> "RCtrl"
        342 -> "LAlt"
        346 -> "RAlt"
        257 -> "Enter"
        256 -> "Esc"
        in 290..301 -> "F${keyCode - 289}"
        else -> {
            GLFW.glfwGetKeyName(keyCode, 0)?.let {
                if (it.length == 1) it.uppercase() else it
            } ?: "K$keyCode"
        }
    }

    companion object { const val HEIGHT = 26f }
}