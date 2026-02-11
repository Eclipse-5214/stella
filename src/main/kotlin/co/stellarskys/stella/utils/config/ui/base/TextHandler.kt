package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.TimeUtils
import co.stellarskys.stella.utils.TimeUtils.millis
import co.stellarskys.stella.utils.config.ui.ConfigUI
import co.stellarskys.stella.utils.render.nvg.Font
import net.minecraft.client.gui.GuiGraphics

class TextHandler(
    private val textProvider: () -> String,
    private val textSetter: (String) -> Unit,
    var font: Font, var fontSize: Float, var textColor: Int,
    var filter: (Char) -> Boolean, var maxLength: Int,
    var centerIfSmall: Boolean = true
) : BaseElement() {
    private val text get() = textProvider()

    private var caret = text.length
        set(value) {
            val clamped = value.coerceIn(0, text.length)
            if (field == clamped) return
            field = clamped
            caretBlinkTime = TimeUtils.now
        }

    private var selection = text.length
    private var selectionWidth = 0f
    private var textOffset = 0f
    private var caretX = 0f
    private var caretBlinkTime = TimeUtils.zero
    private var dragging = false
    var isFocused = false
    var padding = 8f
    var textSidePadding = 8f

    private val history = mutableListOf<String>()
    private var historyIndex = -1

    init {
        saveState()
        updateCaretPosition()
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (dragging && isFocused) {
            caretFromMouse(mouseX)
            updateCaretPosition()
        }

        nvg.pushScissor(x, y, width, height)

        val totalTextWidth = nvg.textWidth(text, fontSize, font)
        val availableWidth = width - (textSidePadding * 2f)
        val centeringOffset = if (centerIfSmall && totalTextWidth < availableWidth) {
            (availableWidth - totalTextWidth) / 2f
        } else 0f

        val renderX = x - textOffset + centeringOffset + textSidePadding
        val textY = y + (height / 2f) - (fontSize / 2f)

        if (selectionWidth != 0f) {
            val selX = nvg.textWidth(text.substring(0, minOf(selection, caret)), fontSize, font)
            nvg.rect(renderX + selX, textY, kotlin.math.abs(selectionWidth), fontSize, 0x665555FF)
        }

        nvg.text(text, renderX, textY, fontSize, textColor, font)

        if (isFocused && caretBlinkTime.since.millis % 1000 < 500) {
            val cx = renderX + caretX
            if (cx in x..(x + width)) nvg.rect(cx, textY, 2f, fontSize, textColor, 1f)
        }

        nvg.popScissor()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        isFocused = isAreaHovered(0f, 0f, width, height, mouseX, mouseY)
        if (isFocused && button == 0) {
            dragging = true
            caretFromMouse(mouseX)
            selection = caret
            updateCaretPosition()
        }
        return isFocused
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) { if (button == 0) dragging = false }

    override fun charTyped(char: Char, modifiers: Int): Boolean {
        if (isFocused && filter(char) && text.length < maxLength) insert(char.toString())
        return isFocused
    }

    override fun keyPressed(keyCode: Int, modifiers: Int): Boolean {
        if (!isFocused) return false
        val ctrl = (modifiers and 2) != 0
        val shift = (modifiers and 1) != 0

        when (keyCode) {
            256 -> isFocused = false
            259 -> if (selection != caret) deleteSelection() else if (caret > 0) {
                textSetter(text.removeRange(caret - 1, caret))
                caret--
                selection = caret
            }
            261 -> if (selection != caret) deleteSelection() else if (caret < text.length) textSetter(text.removeRange(caret, caret + 1))
            263 -> { if (caret > 0) caret--; if (!shift) selection = caret }
            262 -> { if (caret < text.length) caret++; if (!shift) selection = caret }
            65 -> if (ctrl) { selection = 0; caret = text.length }
            90 -> if (ctrl) undo()
        }
        updateCaretPosition()
        return true
    }

    fun updateCaretPosition() {
        caretX = nvg.textWidth(text.substring(0, caret), fontSize, font)
        val anchorX = nvg.textWidth(text.substring(0, selection), fontSize, font)
        selectionWidth = caretX - anchorX

        if (caretX - textOffset > width) textOffset = caretX - width
        if (caretX - textOffset < 0f) textOffset = caretX
    }

    fun onExternalTextUpdate() {
        caret = caret.coerceIn(0, text.length)
        selection = selection.coerceIn(0, text.length)
        updateCaretPosition()
    }

    private fun insert(string: String) {
        if (selection != caret) deleteSelection()
        val result = text.substring(0, caret) + string + text.substring(caret)
        if (result.length <= maxLength) {
            textSetter(result)
            caret += string.length
            selection = caret
            updateCaretPosition()
            saveState()
        }
    }

    private fun deleteSelection() {
        val start = minOf(caret, selection)
        textSetter(text.removeRange(start, maxOf(caret, selection)))
        caret = start
        selection = caret
    }

    private fun caretFromMouse(mouseX: Float) {
        val totalTextWidth = nvg.textWidth(text, fontSize, font)
        val availableWidth = width - (textSidePadding * 2f)
        val centeringOffset = if (centerIfSmall && totalTextWidth < availableWidth) {
            (availableWidth - totalTextWidth) / 2f
        } else 0f

        val localX = mouseX - (absoluteX - textOffset + centeringOffset + textSidePadding)
        caret = (0..text.length).minByOrNull { kotlin.math.abs(nvg.textWidth(text.substring(0, it), fontSize, font) - localX) } ?: 0
    }

    private fun saveState() {
        if (history.lastOrNull() == text) return
        history.add(text)
        historyIndex = history.size - 1
    }

    private fun undo() {
        if (historyIndex > 0) {
            textSetter(history[--historyIndex])
            selection = text.length.also { caret = it }
            updateCaretPosition()
        }
    }
}