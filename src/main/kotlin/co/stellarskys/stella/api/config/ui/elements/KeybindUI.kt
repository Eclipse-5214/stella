package co.stellarskys.stella.api.config.ui.elements

import co.stellarskys.stella.api.config.core.Keybind
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.config.ui.base.ConfigBase
import net.minecraft.client.gui.GuiGraphicsExtractor
import org.lwjgl.glfw.GLFW

class KeybindUI(initX: Float, initY: Float, val keybind: Keybind): ConfigBase(initX, initY, keybind, 52f) {
    private var isListening = false
    private val handler = keybind.value as Keybind.Handler

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        if (!value) isListening = false // Stop listening if hidden
    }

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        // Keybind Box
        val boxW = 40f
        val boxH = 28f
        val boxX = width - boxW - 12f
        val boxY = (HEIGHT - boxH) / 2f

        val isHovered = isAreaHovered(boxX, boxY, boxW, boxH, mouseX, mouseY)

        // Render the button
        val bgColor = if (isListening) Palette.Purple.withAlpha(100).rgb else Palette.Base.rgb
        val borderColor = if (isListening || isHovered) Palette.Purple.rgb else Palette.Purple.withAlpha(80).rgb

        nvg.rect(boxX, boxY, boxW, boxH, bgColor, 6f)
        nvg.hollowRect(boxX, boxY, boxW, boxH, 2f, borderColor, 6f)

        // Key Text
        val displayStr = if (isListening) "..." else getKeyStringName(handler.keyCode())
        val fontSize = 14f
        val textWidth = nvg.textWidth(displayStr, fontSize, nvg.inter)
        val textX = boxX + (boxW - textWidth) / 2f
        val textY = boxY + (boxH / 2f) - (fontSize / 2f)
        nvg.text(displayStr, textX, textY, fontSize, Palette.Text.rgb, nvg.inter)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.canReceiveInput  == false || !visible || offset > 1f || parent?.isAnimating == true) return false

        val boxW = 40f
        val boxH = 28f
        val boxX = width - boxW - 12f
        val boxY = (HEIGHT - boxH) / 4f

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
}