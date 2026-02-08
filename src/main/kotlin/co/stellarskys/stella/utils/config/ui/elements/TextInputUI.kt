package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config.core.TextInput
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import co.stellarskys.stella.utils.config.ui.base.TextBox
import net.minecraft.client.gui.GuiGraphics

class TextInputUI(initX: Float, initY: Float, val input: TextInput) : BaseElement() {
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var offset by offsetAnim

    private val textField: TextBox = TextBox(
        x = 8f,
        y = 17f,
        w = 104f,
        h = 12f,
        initialText = input.value as String,
        onType = { str ->
            input.value = str
            input.onValueChanged?.invoke(str)
        },
        fontSize = 7f,
        color = Palette.Base.rgb,
        borderColor = Palette.Purple.withAlpha(50).rgb,
        focusColor = Palette.Purple.rgb,
        maxLength = 64
    ).apply { parent = this@TextInputUI }

    init {
        x = initX; y = initY; width = 120f
        offset = if (visible) 0f else HEIGHT; height = HEIGHT - offset
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return

        if (isAnimating) {
            height = (HEIGHT - offset).coerceAtLeast(0f)
            if (offsetAnim.done()) isAnimating = false
        }

        nvg.push(); nvg.translate(x, y); nvg.pushScissor(0f, 0f, width, height)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(input.name, 6f, 7f, 8f, Palette.Text.rgb, nvg.inter)
        textField.render(context, mouseX , mouseY , delta)

        nvg.popScissor(); nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible || offset > 1f) return false
        if (textField.mouseClicked(mouseX , mouseY , button)) return true
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        offset = if (value) 0f else HEIGHT
        isAnimating = true
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        textField.mouseReleased(mouseX , mouseY , button)
    }

    override fun charTyped(char: Char, modifiers: Int) = textField.charTyped(char, modifiers)
    override fun keyPressed(keyCode: Int, modifiers: Int) = textField.keyPressed(keyCode, modifiers)

    companion object { const val HEIGHT = 36f }
}