package co.stellarskys.stella.api.config.ui.elements

import co.stellarskys.stella.api.config.core.TextInput
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.config.ui.base.ConfigBase
import co.stellarskys.stella.api.horizon.nvg.TextBox
import net.minecraft.client.gui.GuiGraphicsExtractor

class TextInputUI(initX: Float, initY: Float, val input: TextInput): ConfigBase(initX, initY, input, 72f) {
    private val textField: TextBox = TextBox(
        x = 16f, y = 34f, w = 208f, h = 24f,
        initialText = input.value as String,
        onType = { str ->
            input.value = str
            input.onValueChanged?.invoke(str)
        },
        fontSize = 14f,
        color = Palette.Base.rgb,
        borderColor = Palette.Purple.withAlpha(50).rgb,
        focusColor = Palette.Purple.rgb,
        maxLength = 64
    ).apply { parent = this@TextInputUI }

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        textField.render(context, mouseX , mouseY , delta)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.canReceiveInput  == false || !visible || offset > 1f || parent?.isAnimating == true) return false
        textField.mouseClicked(mouseX , mouseY , button)
        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        textField.mouseReleased(mouseX , mouseY , button)
    }

    override fun charTyped(char: Char) = textField.charTyped(char)
    override fun keyPressed(keyCode: Int, modifiers: Int) = textField.keyPressed(keyCode, modifiers)
}