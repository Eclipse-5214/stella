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
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var expansion by expansionAnim
    private var offset by offsetAnim
    private var hsb = FloatArray(3)
    private var alpha = (picker.value as Color).alpha / 255f
    private var draggingArea = false
    private var draggingHue = false
    private var draggingAlpha = false
    private val recentColors = mutableListOf<Color>()

    private val hexBox = TextBox(
        16f, 0f, 96f, 24f, (picker.value as Color).toHex(),
        fontSize = 16f, borderColor = Palette.Purple.withAlpha(150).rgb, focusColor = Palette.Purple.rgb,
        filter = { it.isDigit() || it.lowercaseChar() in 'a'..'f' || it == '#' }, maxLength = 9
    ) { newHex ->
        try { applyColor(Utils.colorFromHex(newHex), false) } catch (_: Exception) {}
    }

    init {
        x = initX
        y = initY
        width = 240f
        offset = if (visible) 0f else HEIGHT
        height = HEIGHT - offset
        expansion = 0f
        Color.RGBtoHSB((picker.value as Color).red, (picker.value as Color).green, (picker.value as Color).blue, hsb)
        hexBox.parent = this
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        offset = if (value) 0f else (HEIGHT + (CONTENT_HEIGHT * expansion))
        isAnimating = true
    }

    private fun applyColor(color: Color, updateHex: Boolean = true) {
        Color.RGBtoHSB(color.red, color.green, color.blue, hsb)
        alpha = color.alpha / 255f
        if (updateHex) hexBox.setText(color.toHex())
        updatePickerValue(updateHex)
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return

        val currentFullHeight = HEIGHT + (CONTENT_HEIGHT * expansion)

        if (isAnimating) {
            height = (currentFullHeight - offset).coerceAtLeast(0f)
            if (expansionAnim.done() && offsetAnim.done()) isAnimating = false
        }

        nvg.push()
        nvg.translate(x, y)
        nvg.pushScissor(0f, 0f, width, height)

        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(picker.name, 12f, 17f, 16f, Palette.Text.rgb, nvg.inter)
        nvg.rect(width - 40f, 11f, 28f, 28f, (picker.value as Color).rgb, 14f)

        if (expansion > 0.01f) {
            nvg.pushScissor(0f, HEIGHT, width, height - HEIGHT)
            nvg.rect(0f, HEIGHT, width, height - HEIGHT, Palette.Crust.withAlpha(100).rgb)
            val startY = HEIGHT + 12f

            nvg.push()
            nvg.translate(16f, startY)
            drawRoundedSBArea(PICKER_SIZE)
            nvg.translate(PICKER_SIZE + GAP, 0f); drawVerticalHueSlider(SLIDER_WIDTH, PICKER_SIZE)
            nvg.translate(SLIDER_WIDTH + GAP, 0f); drawVerticalAlphaSlider(SLIDER_WIDTH, PICKER_SIZE)
            nvg.pop()

            val rowY = startY + PICKER_SIZE + 16f
            hexBox.apply { y = rowY; render(context, mouseX, mouseY, delta) }

            var rx = 16f + hexBox.width + 20f
            recentColors.forEach { nvg.rect(rx, rowY + 2f, 24f, 24f, it.rgb, 12f); nvg.hollowRect(rx, rowY + 2f, 24f, 24f, 2f, Palette.Purple.withAlpha(150).rgb, 12f); rx += 32f }
            nvg.popScissor()
        }

        nvg.popScissor()
        nvg.pop()

        if (draggingArea || draggingHue || draggingAlpha) updateFromMouse(mouseX, mouseY)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.canReceiveInput  == false || parent?.isAnimating == true || !visible) return false
        if (isAreaHovered(0f, 0f, width, HEIGHT)) { expansion = if (expansion > 0.5f) 0f else 1f; isAnimating = true; return true }
        if (expansion <= 0.5f) return false
        hexBox.mouseClicked(mouseX, mouseY, button)

        val startY = HEIGHT + 12f; val rowY = startY + PICKER_SIZE + 16f
        var rx = 16f + hexBox.width + 20f
        recentColors.forEach { if (isAreaHovered(rx, rowY, 24f, 24f, mouseX, mouseY)) { applyColor(it); return true }; rx += 32f }

        if (isAreaHovered(16f, startY, PICKER_SIZE, PICKER_SIZE)) draggingArea = true
        else if (isAreaHovered(16f + PICKER_SIZE + GAP, startY, SLIDER_WIDTH, PICKER_SIZE)) draggingHue = true
        else if (isAreaHovered(16f + PICKER_SIZE + GAP + SLIDER_WIDTH + GAP, startY, SLIDER_WIDTH, PICKER_SIZE)) draggingAlpha = true
        else return false
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        if (!visible) return
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
        val startY = HEIGHT + 12f
        val localY = (my - (absoluteY + startY)).coerceIn(0f, PICKER_SIZE)
        if (draggingArea) {
            hsb[1] = (mx - (absoluteX + 16f)).coerceIn(0f, PICKER_SIZE) / PICKER_SIZE
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
        nvg.gradientRect(0f, 0f, s, s, -1, Color.HSBtoRGB(hsb[0], 1f, 1f), Gradient.LeftToRight, 6f)
        nvg.gradientRect(0f, 0f, s, s, 0, 0xFF000000.toInt(), Gradient.TopToBottom, 6f)
        nvg.hollowRect(hsb[1] * s - 3f, (1f - hsb[2]) * s - 3f, 6f, 6f, 2f, -1, 7f)
    }

    private fun drawVerticalHueSlider(w: Float, h: Float) {
        val s = h / 6f
        for (i in 0..5) nvg.gradientRect(0f, i * s, w, s + 1f, Color.HSBtoRGB(i/6f, 1f, 1f), Color.HSBtoRGB((i+1)/6f, 1f, 1f), Gradient.TopToBottom)
        nvg.rect(-2f, hsb[0] * h - 3f, w + 4f, 6f, -1, 2f)
    }

    private fun drawVerticalAlphaSlider(w: Float, h: Float) {
        nvg.pushScissor(0f, 0f, w, h)
        nvg.push()
        nvg.rect(0f, 0f, w, h, -1)
        for (i in 0..(h / (w / 2f)).toInt()) nvg.rect(if (i % 2 == 0) 0f else w / 2f, i * (w / 2f), w / 2f, w / 2f, 0xFFCCCCCC.toInt())
        nvg.pop()
        nvg.gradientRect(0f, 0f, w, h, Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])).withAlpha(0).rgb, Color(Color.HSBtoRGB(hsb[0], hsb[1], hsb[2])).withAlpha(255).rgb, Gradient.TopToBottom)
        nvg.rect(-2f, alpha * h - 3f, w + 4f, 6f, -1, 2f)
        nvg.popScissor()
    }

    companion object {
        const val HEIGHT = 50f;
        const val CONTENT_HEIGHT = 220f;
        const val PICKER_SIZE = 144f
        const val SLIDER_WIDTH = 20f;
        const val GAP = 12f;
        const val MAX_RECENT = 3
    }
}