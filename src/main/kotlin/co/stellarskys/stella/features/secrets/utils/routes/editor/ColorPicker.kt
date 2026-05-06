package co.stellarskys.stella.features.secrets.utils.routes.editor

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.config.ui.base.BaseElement
import co.stellarskys.stella.api.config.ui.base.TextBox
import co.stellarskys.stella.api.nvg.Gradient
import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.Utils.toHex
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class ColorPicker(pickerWidth: Float) : BaseElement() {
    private var hsb = floatArrayOf(0f, 0.8f, 1f)
    private var draggingArea = false
    private var draggingHue = false

    val currentColor get(): Color = Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]))

    val hexBox: TextBox = TextBox(
        0f, PICKER_SIZE + 10f, 100f, 24f,
        initialText = "",
        fontSize = 14f,
        borderColor = Palette.Purple.withAlpha(150).rgb,
        focusColor = Palette.Purple.rgb,
        filter = { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == '#' },
        maxLength = 7,
        onType = { hex ->
            try { applyColor(Utils.colorFromHex(hex), updateHex = false) } catch (_: Exception) {}
        }
    )

    init {
        width = pickerWidth
        height = PICKER_HEIGHT
        hexBox.parent = this
        hexBox.setText(currentColor.toHex(includeAlpha = false))
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (draggingArea || draggingHue) updateFromMouse(mouseX, mouseY)

        nvg.push()
        nvg.translate(x, y)
        drawSBArea(0f, 0f, PICKER_SIZE)
        drawHueSlider(PICKER_SIZE + GAP, 0f, SLIDER_W, PICKER_SIZE)
        hexBox.render(context, mouseX, mouseY, delta)
        drawSwatch(108f, PICKER_SIZE + 10f, 30f, 30f)
        nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (hexBox.mouseClicked(mouseX, mouseY, button)) return true
        val hx = PICKER_SIZE + GAP
        if (isAreaHovered(0f, 0f, PICKER_SIZE, PICKER_SIZE)) { draggingArea = true; return true }
        if (isAreaHovered(hx, 0f, SLIDER_W, PICKER_SIZE)) { draggingHue = true; return true }
        return false
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        draggingArea = false; draggingHue = false
        hexBox.mouseReleased(mouseX, mouseY, button)
    }

    override fun charTyped(char: Char) = hexBox.charTyped(char)
    override fun keyPressed(keyCode: Int, modifiers: Int) = hexBox.keyPressed(keyCode, modifiers)

    private fun applyColor(color: Color, updateHex: Boolean = true) {
        Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
        if (updateHex) hexBox.setText(color.toHex(includeAlpha = false))
    }

    private fun updateFromMouse(mx: Float, my: Float) {
        val relY = (my - absoluteY).coerceIn(0f, PICKER_SIZE)
        when {
            draggingArea -> {
                hsb[1] = (mx - absoluteX).coerceIn(0f, PICKER_SIZE) / PICKER_SIZE
                hsb[2] = 1f - relY / PICKER_SIZE
            }
            draggingHue -> hsb[0] = relY / PICKER_SIZE
        }
        if (!hexBox.isFocused) hexBox.setText(currentColor.toHex(includeAlpha = false))
    }

    private fun drawSBArea(lx: Float, ly: Float, s: Float) {
        nvg.gradientRect(lx, ly, s, s, -1, Color.HSBtoRGB(hsb[0], 1f, 1f), Gradient.LeftToRight, 6f)
        nvg.gradientRect(lx, ly, s, s, 0, 0xFF000000.toInt(), Gradient.TopToBottom, 6f)
        nvg.hollowRect(lx + hsb[1] * s - 3f, ly + (1f - hsb[2]) * s - 3f, 6f, 6f, 2f, -1, 7f)
    }

    private fun drawHueSlider(lx: Float, ly: Float, w: Float, h: Float) {
        val step = h / 6f
        for (i in 0..5) nvg.gradientRect(lx, ly + i * step, w, step + 1f, Color.HSBtoRGB(i / 6f, 1f, 1f), Color.HSBtoRGB((i + 1) / 6f, 1f, 1f), Gradient.TopToBottom)
        nvg.rect(lx - 2f, ly + hsb[0] * h - 3f, w + 4f, 6f, -1, 2f)
    }

    private fun drawSwatch(lx: Float, ly: Float, w: Float, h: Float) {
        nvg.rect(lx, ly, w, h, currentColor.rgb, 4f)
        nvg.hollowRect(lx, ly, w, h, 1f, Palette.Surface1.rgb, 4f)
    }

    companion object {
        const val PICKER_SIZE = 130f
        const val SLIDER_W = 18f
        const val GAP = 10f
        const val PICKER_HEIGHT = PICKER_SIZE + 44f
    }
}
