package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.Utils.toHex
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.ColorPicker
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import co.stellarskys.stella.utils.config.ui.base.TextBox
import co.stellarskys.stella.utils.render.nvg.Gradient
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class ColorPickerUI(initX: Float, initY: Float, val picker: ColorPicker) : BaseElement() {
    private var expansionAnim = Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var expansion by expansionAnim

    private var hsb = FloatArray(3)
    private var alpha = (picker.value as Color).alpha / 255f
    private var draggingArea = false; private var draggingHue = false; private var draggingAlpha = false
    private val recentColors = mutableListOf<Color>()

    private val hexBox = TextBox(
        8f, 0f, 48f, 12f, (picker.value as Color).toHex(),
        fontSize = 8f, borderColor = Palette.Purple.withAlpha(150).rgb, focusColor = Palette.Purple.rgb,
        filter = { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == '#' }, maxLength = 9
    ) { newHex ->
        try { applyColor(Utils.colorFromHex(newHex), false) } catch (_: Exception) {}
    }

    init {
        x = initX;
        y = initY;
        width = 120f;
        height = HEIGHT
        expansion = 0f
        Color.RGBtoHSB((picker.value as Color).red, (picker.value as Color).green, (picker.value as Color).blue, hsb)
        hexBox.parent = this
    }

    private fun applyColor(color: Color, updateHex: Boolean = true) {
        Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
        alpha = color.alpha / 255f
        if (updateHex) hexBox.setText(color.toHex())
        updatePickerValue(updateHex)
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return
        if (isAnimating) {
            height = HEIGHT + (CONTENT_HEIGHT * expansion)
            if (expansionAnim.done()) isAnimating = false
        }

        nvg.push()
        nvg.translate(x, y)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(picker.name, 6f, 8.5f, 8f, Palette.Text.rgb, nvg.inter)
        nvg.rect(width - 20f, 5.5f, 14f, 14f, (picker.value as Color).rgb, 7f)

        if (expansion > 0.01f) {
            nvg.pushScissor(0f, HEIGHT, width, height - HEIGHT)
            nvg.rect(0f, HEIGHT, width, height - HEIGHT, Palette.Crust.withAlpha(100).rgb)
            val startY = HEIGHT + 6f

            nvg.push()
            nvg.translate(8f, startY)
            drawRoundedSBArea(PICKER_SIZE)
            nvg.translate(PICKER_SIZE + GAP, 0f); drawVerticalHueSlider(SLIDER_WIDTH, PICKER_SIZE)
            nvg.translate(SLIDER_WIDTH + GAP, 0f); drawVerticalAlphaSlider(SLIDER_WIDTH, PICKER_SIZE)
            nvg.pop()

            val rowY = startY + PICKER_SIZE + 8f
            hexBox.apply { y = rowY; render(context, mouseX, mouseY, delta) }

            var rx = 8f + hexBox.width + 10f
            recentColors.forEach { nvg.rect(rx, rowY + 1f, 12f, 12f, it.rgb, 6f); nvg.hollowRect(rx, rowY + 1f, 12f, 12f, 1f, Palette.Purple.withAlpha(150).rgb, 6f); rx += 16f }
            nvg.popScissor()
        }
        nvg.pop()
        if (draggingArea || draggingHue || draggingAlpha) updateFromMouse(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.isAnimating == true || !visible) return false
        if (isAreaHovered(0f, 0f, width, HEIGHT)) { expansion = if (expansion > 0.5f) 0f else 1f; isAnimating = true; return true }
        if (expansion <= 0.5f) return false
        if (hexBox.mouseClicked(mouseX, mouseY, button)) return true

        val startY = HEIGHT + 6f; val rowY = startY + PICKER_SIZE + 8f
        var rx = 8f + hexBox.width + 10f
        recentColors.forEach { if (isAreaHovered(rx, rowY, 12f, 12f, mouseX, mouseY)) { applyColor(it); return true }; rx += 16f }

        if (isAreaHovered(8f, startY, PICKER_SIZE, PICKER_SIZE)) draggingArea = true
        else if (isAreaHovered(8f + PICKER_SIZE + GAP, startY, SLIDER_WIDTH, PICKER_SIZE)) draggingHue = true
        else if (isAreaHovered(8f + PICKER_SIZE + GAP + SLIDER_WIDTH + GAP, startY, SLIDER_WIDTH, PICKER_SIZE)) draggingAlpha = true
        else return false
        return true
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        if (parent?.isAnimating == true || !visible) return
        if (draggingArea || draggingHue || draggingAlpha) saveToHistory(picker.value as Color)
        draggingArea = false; draggingHue = false; draggingAlpha = false
        hexBox.mouseReleased(mouseX, mouseY, button)
    }

    private fun updatePickerValue(updateTextBox: Boolean = true) {
        val rgb = Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])
        val finalColor = Color(Color(rgb).red, Color(rgb).green, Color(rgb).blue, (alpha * 255).toInt())
        picker.value = finalColor
        if (updateTextBox && !hexBox.isFocused) hexBox.setText(finalColor.toHex())
    }

    private fun updateFromMouse(mx: Float, my: Float) {
        val startY = HEIGHT + 6f
        val localY = (my - (absoluteY + startY)).coerceIn(0f, PICKER_SIZE)
        if (draggingArea) {
            hsb[1] = (mx - (absoluteX + 8f)).coerceIn(0f, PICKER_SIZE) / PICKER_SIZE
            hsb[2] = 1f - (localY / PICKER_SIZE)
        } else if (draggingHue) hsb[0] = localY / PICKER_SIZE
        else if (draggingAlpha) alpha = localY / PICKER_SIZE
        updatePickerValue()
    }

    private fun saveToHistory(c: Color) {
        if (recentColors.any { it.rgb == c.rgb }) return
        recentColors.add(0, c)
        if (recentColors.size > MAX_RECENT) recentColors.removeLast()
    }

    override fun charTyped(char: Char, modifiers: Int) = hexBox.charTyped(char, modifiers)
    override fun keyPressed(keyCode: Int, modifiers: Int) = hexBox.keyPressed(keyCode, modifiers)

    private fun drawRoundedSBArea(s: Float) {
        nvg.gradientRect(0f, 0f, s, s, -1, Color.HSBtoRGB(hsb[0], 1f, 1f), Gradient.LeftToRight, 3f)
        nvg.gradientRect(0f, 0f, s, s, 0, 0xFF000000.toInt(), Gradient.TopToBottom, 3f)
        nvg.hollowRect(hsb[1] * s - 1.5f, (1f - hsb[2]) * s - 1.5f, 3f, 3f, 1f, -1, 3.5f)
    }

    private fun drawVerticalHueSlider(w: Float, h: Float) {
        val s = h / 6f
        for (i in 0..5) nvg.gradientRect(0f, i * s, w, s + 0.5f, Color.HSBtoRGB(i/6f, 1f, 1f), Color.HSBtoRGB((i+1)/6f, 1f, 1f), Gradient.TopToBottom)
        nvg.rect(-1f, hsb[0] * h - 1.5f, w + 2f, 3f, -1, 1f)
    }

    private fun drawVerticalAlphaSlider(w: Float, h: Float) {
        nvg.pushScissor(0f, 0f, w, h)
        nvg.push()
        nvg.rect(0f, 0f, w, h, -1)
        for (i in 0..(h / (w / 2f)).toInt()) nvg.rect(if (i % 2 == 0) 0f else w / 2f, i * (w / 2f), w / 2f, w / 2f, 0xFFCCCCCC.toInt())
        nvg.pop()
        nvg.gradientRect(0f, 0f, w, h, Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])).withAlpha(0).rgb, Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])).withAlpha(255).rgb, Gradient.TopToBottom)
        nvg.rect(-1f, alpha * h - 1.5f, w + 2f, 3f, -1, 1f)
        nvg.popScissor()
    }

    companion object {
        const val HEIGHT = 25f;
        const val CONTENT_HEIGHT = 110f;
        const val PICKER_SIZE = 72f
        const val SLIDER_WIDTH = 10f;
        const val GAP = 6f;
        const val MAX_RECENT = 3
    }
}