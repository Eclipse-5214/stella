package co.stellarskys.stella.api.horizon.mc

import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis
import co.stellarskys.stella.api.zenith.client
import net.minecraft.client.gui.GuiGraphicsExtractor
import co.stellarskys.stella.api.zenith.Zenith.Keys
import co.stellarskys.stella.api.zenith.Zenith.Keys.isCtrlDown
import co.stellarskys.stella.api.zenith.Zenith.Keys.isShiftDown
import java.awt.Color
import kotlin.math.abs

class TextHandler(
    private val textProvider: () -> String,
    private val textSetter: (String) -> Unit,
    var scale: Float = 1f,
    var textColor: Color = Color.WHITE,
    var filter: (Char) -> Boolean = { true },
    var maxLength: Int = 100,
    var centerIfSmall: Boolean = true
) : BaseElement() {
    private val text get() = textProvider()

    private var caret = text.length
        set(value) {
            val clamped = value.coerceIn(0, text.length)
            if (field == clamped) return
            field = clamped
            caretBlinkTime = Chronos.now
        }

    private var selection = text.length
    private var selectionWidth = 0f
    private var textOffset = 0f
    private var caretX = 0f
    private var caretBlinkTime = Chronos.zero
    private var dragging = false
    var isFocused = false
    var textSidePadding = 8f

    private val history = mutableListOf<String>()
    private var historyIndex = -1

    private var cachedPosText = ""
    private var cachedPosCaret = -1
    private var cachedPosSelection = -1
    private var cachedPosScale = -1f

    init {
        saveState()
        updateCaretPosition()
    }

    private fun textWidth(str: String): Float = client.font.width(str) * scale
    private val fontHeight: Float get() = client.font.lineHeight * scale

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        if (dragging && isFocused) {
            caretFromMouse(mouseX)
            updateCaretPosition()
        }

        context.enableScissor(0, 0, width.toInt(), height.toInt())

        val totalTextWidth = textWidth(text)
        val availableWidth = width - (textSidePadding * 2f)
        val centeringOffset = if (centerIfSmall && totalTextWidth < availableWidth) {
            (availableWidth - totalTextWidth) / 2f
        } else 0f

        val renderX = 0f - textOffset + centeringOffset + textSidePadding
        val textY = 0f + (height / 2f) - (fontHeight / 2f)

        if (selectionWidth != 0f) {
            val selX = textWidth(text.substring(0, minOf(selection, caret)))
            ren2d.drawRect(context, (renderX + selX).toInt(), textY.toInt(), abs(selectionWidth).toInt(), fontHeight.toInt(), Color(102, 85, 85, 255))
        }

        ren2d.drawString(context, text, renderX.toInt(), textY.toInt(), scale, textColor)

        if (isFocused && caretBlinkTime.since.millis % 1000 < 500) {
            val cx = renderX + caretX
            if (cx in 0f..width) {
                ren2d.drawRect(context, cx.toInt(), textY.toInt(), 2, fontHeight.toInt(), textColor)
            }
        }

        context.disableScissor()
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

    override fun charTyped(char: Char): Boolean {
        if (isFocused && filter(char) && text.length < maxLength) insert(char.toString())
        return isFocused
    }

    override fun keyPressed(keyCode: Int, modifiers: Int): Boolean {
        if (!isFocused) return false
        val ctrl = modifiers.isCtrlDown
        val shift = modifiers.isShiftDown
        val kh = client.keyboardHandler

        when (keyCode) {
            Keys.ESCAPE -> isFocused = false // Escape
            Keys.BACKSPACE -> if (selection != caret) deleteSelection() else if (caret > 0) { // Backspace
                if (ctrl) {
                    val textToSearch = text.substring(0, caret)
                    var boundary = textToSearch.trimEnd().lastIndexOf(' ')
                    if (boundary == -1) {
                        boundary = 0
                    } else {
                        boundary += 1
                    }
                    textSetter(text.removeRange(boundary, caret))
                    caret = boundary
                    selection = caret
                } else {
                    textSetter(text.removeRange(caret - 1, caret))
                    caret--
                    selection = caret
                }
            }
            Keys.DELETE -> if (selection != caret) deleteSelection() else if (caret < text.length) textSetter(text.removeRange(caret, caret + 1)) // Delete
            Keys.LEFT -> { if (caret > 0) caret--; if (!shift) selection = caret } // Left
            Keys.RIGHT -> { if (caret < text.length) caret++; if (!shift) selection = caret } // Right
            Keys.A -> if (ctrl) { selection = 0; caret = text.length } // Ctrl+A
            Keys.C -> if (ctrl && selection != caret) { // Ctrl+C
                val start = minOf(caret, selection)
                val end = maxOf(caret, selection)
                kh.clipboard = text.substring(start, end)
            }
            Keys.X -> if (ctrl && selection != caret) { // Ctrl+X
                val start = minOf(caret, selection)
                val end = maxOf(caret, selection)
                kh.clipboard = text.substring(start, end)
                deleteSelection()
            }
            Keys.V -> if (ctrl) { // Ctrl+V
                val content = kh.clipboard
                    .replace("\n", "")
                    .replace("\r", "")
                insert(content)
            }
            Keys.Z -> if (ctrl) undo() // Ctrl+Z
        }
        updateCaretPosition()
        return true
    }

    fun updateCaretPosition() {
        val curText = text
        if (curText != cachedPosText || caret != cachedPosCaret || selection != cachedPosSelection || scale != cachedPosScale) {
            cachedPosText = curText
            cachedPosCaret = caret
            cachedPosSelection = selection
            cachedPosScale = scale
            caretX = textWidth(curText.substring(0, caret))
            val anchorX = textWidth(curText.substring(0, selection))
            selectionWidth = caretX - anchorX
        }

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
        val totalTextWidth = textWidth(text)
        val availableWidth = width - (textSidePadding * 2f)
        val centeringOffset = if (centerIfSmall && totalTextWidth < availableWidth) {
            (availableWidth - totalTextWidth) / 2f
        } else 0f

        val localX = mouseX - (absoluteX - textOffset + centeringOffset + textSidePadding)
        caret = (0..text.length).minByOrNull { abs(textWidth(text.substring(0, it)) - localX) } ?: 0
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
