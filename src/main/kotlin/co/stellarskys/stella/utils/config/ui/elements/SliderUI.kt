package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.Slider
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import co.stellarskys.stella.utils.config.ui.base.TextBox
import net.minecraft.client.gui.GuiGraphics

class SliderUI(initX: Float, initY: Float, val slider: Slider) : BaseElement() {
    private var visualProgressAnim = Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var visualProgress by visualProgressAnim
    private var offset by offsetAnim
    private var dragging = false
    private var lastFocusState = false

    private val valueInput: TextBox = TextBox(
        x = 0f, y = 8f, w = 56f, h = 20f,
        initialText = String.format("%.2f", slider.value as Float),
        onType = { str ->
            str.toFloatOrNull()?.let {
                slider.value = it.coerceIn(slider.min, slider.max)
            }
        },
        fontSize = 14f,
        color = Palette.Base.rgb,
        borderColor = Palette.Purple.withAlpha(50).rgb,
        focusColor = Palette.Purple.rgb
    ).apply { parent = this@SliderUI }

    init {
        x = initX; y = initY
        offset = if (visible) 0f else HEIGHT; height = HEIGHT - offset
        visualProgress = (slider.value as Float - slider.min) / (slider.max - slider.min)
        valueInput.x = width - valueInput.width - 12f
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return
        if (lastFocusState && !valueInput.isFocused) valueInput.setText(String.format("%.2f", slider.value as Float))
        if (dragging || !valueInput.isFocused) valueInput.setText(String.format("%.2f", slider.value as Float))
        lastFocusState = valueInput.isFocused

        if (isAnimating) {
            height = (HEIGHT - offset).coerceAtLeast(0f)
            if (offsetAnim.done()) isAnimating = false
        }

        visualProgress = (slider.value as Float - slider.min) / (slider.max - slider.min)

        val trackX = 16f; val trackW = width - 32f; val trackY = HEIGHT - 16f
        val knobX = trackX + (trackW * visualProgress)

        nvg.push(); nvg.translate(x, y); nvg.pushScissor(0f, 0f, width, height)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(slider.name, 12f, 14f, 16f, Palette.Text.rgb, nvg.inter)

        valueInput.render(context, mouseX, mouseY, delta)

        // Slider Track & Knob
        nvg.rect(trackX, trackY, trackW, 4f, Palette.Base.rgb, 2f)
        nvg.rect(trackX, trackY, trackW * visualProgress, 4f, Palette.Purple.rgb, 2f)
        nvg.rect(knobX - 6f, trackY - 4f, 12f, 12f, Palette.Text.rgb, 6f)

        if (dragging || !visualProgressAnim.done()) {
            val alpha = if (dragging) 150 else 0
            if (alpha > 0) nvg.hollowRect(knobX - 8f, trackY - 6f, 16f, 16f, 2f, Palette.Purple.withAlpha(alpha).rgb, 8f)
        }

        nvg.popScissor(); nvg.pop()
        if (dragging) updateValue(mouseX)
    }

    private fun updateValue(mouseX: Float) {
        val trackX = absoluteX + 16f
        val trackW = width - 32f
        val percent = ((mouseX - trackX) / trackW).coerceIn(0f, 1f)
        slider.value = slider.min + percent * (slider.max - slider.min)
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        offset = if (value) 0f else HEIGHT
        isAnimating = true
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible || offset > 1f || parent?.isAnimating == true) return false

        if (valueInput.mouseClicked(mouseX, mouseY, button)) return true

        if (isAreaHovered(12f, HEIGHT / 2f, width - 24f, HEIGHT / 2f, mouseX, mouseY)) {
            valueInput.isFocused = false
            dragging = true; updateValue(mouseX); return true
        }

        valueInput.isFocused = false
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        dragging = false
        valueInput.mouseReleased(mouseX, mouseY, button)
    }

    override fun charTyped(char: Char, modifiers: Int) = valueInput.charTyped(char, modifiers)
    override fun keyPressed(keyCode: Int, modifiers: Int) = valueInput.keyPressed(keyCode, modifiers)

    companion object { const val HEIGHT = 60f }
}