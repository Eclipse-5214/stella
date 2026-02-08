package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.render.nvg.Font
import co.stellarskys.stella.utils.render.nvg.NVGRenderer
import net.minecraft.client.gui.GuiGraphics

class TextBox(
    x: Float, y: Float, w: Float, h: Float,
    initialText: String,
    var color: Int = 0xAA111111.toInt(),
    var textColor: Int = 0xFFFFFFFF.toInt(),
    var font: Font = NVGRenderer.inter,
    var fontSize: Float = 9f,
    var radius: Float = 4f,
    var borderColor: Int = 0xFF333333.toInt(),
    var focusColor: Int = 0xFF5555FF.toInt(),
    var borderWidth: Float = 1f,
    var maxLength: Int = 32,
    var filter: (Char) -> Boolean = { true },
    val onType: (String) -> Unit
) : BaseElement() {
    private var padding = borderWidth + 3f
    private var currentText = initialText

    init {
        this.x = x
        this.y = y
        this.width = w
        this.height = h.coerceAtLeast(fontSize + padding * 2f)
    }

    private val handler = TextHandler(
        textProvider = { currentText },
        textSetter = {
            currentText = it
            onType(it)
        },
        font = font,
        fontSize = fontSize,
        textColor = textColor,
        filter = filter,
        maxLength = maxLength
    ).apply {
        this.parent = this@TextBox
        this.width = w - (padding * 2f)
        this.height = fontSize
        this.x = padding
        this.y = (this@TextBox.height / 2f) - (fontSize / 2)
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        nvg.push()
        nvg.translate(x, y)
        nvg.rect(0f, 0f, width, height, color, radius)

        if (borderWidth > 0f) {
            val currentStrokeColor = if (handler.isFocused) focusColor else borderColor
            nvg.hollowRect(0f, 0f, width, height, borderWidth, currentStrokeColor, radius)
        }

        handler.render(context, mouseX, mouseY, delta)
        nvg.pop()
    }

    fun setText(newText: String) { currentText = newText; handler.updateCaretPosition() }
    var isFocused: Boolean by handler::isFocused

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int) = handler.mouseClicked(mouseX, mouseY, button)
    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) = handler.mouseReleased(mouseX, mouseY, button)
    override fun charTyped(char: Char, modifiers: Int) = handler.charTyped(char, modifiers)
    override fun keyPressed(keyCode: Int, modifiers: Int) = handler.keyPressed(keyCode, modifiers)
}