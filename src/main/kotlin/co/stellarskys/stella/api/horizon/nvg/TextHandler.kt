package co.stellarskys.stella.api.horizon.nvg

import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis
import co.stellarskys.stella.api.luminav2.types.LuminaFont
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.api.zenith.Zenith.Keys
import co.stellarskys.stella.api.zenith.Zenith.Keys.isCtrlDown
import co.stellarskys.stella.api.zenith.Zenith.Keys.isShiftDown
import co.stellarskys.stella.api.zenith.client
import net.minecraft.client.gui.GuiGraphicsExtractor
import kotlin.math.abs

/*
 * Adapted from TextInputHandler.kt in OdinFabric
 * https://github.com/odtheking/OdinFabric
 *
 * BSD 3-Clause License
 * Copyright (c) 2025, odtheking
 * See full license at: https://opensource.org/licenses/BSD-3-Clause
 */

class TextHandler(
    private val textProvider: () -> String,
    private val textSetter: (String) -> Unit,
    var font: LuminaFont, var fontSize: Float, var textColor: Int,
    var filter: (Char) -> Boolean, var maxLength: Int,
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
    var padding = 8f
    var textSidePadding = 8f

    var isFocused = false
        set(value) {
            if (field == value) return
            field = value
            applyTextInput(value)
        }

    private val history = mutableListOf<String>()
    private var historyIndex = -1

    private var cachedPosText = ""
    private var cachedPosCaret = -1
    private var cachedPosSelection = -1
    private var cachedPosFontSize = -1f
    private var cachedPosFont: LuminaFont? = null

    init {
        saveState()
        updateCaretPosition()
    }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
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
            nvg.rect(renderX + selX, textY, abs(selectionWidth), fontSize, 0x665555FF)
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
        if (isFocused && button == Zenith.Mouse.LEFT) {
            dragging = true
            caretFromMouse(mouseX)
            selection = caret
            updateCaretPosition()
        }
        return isFocused
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) { if (button == Zenith.Mouse.LEFT) dragging = false }

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
            Keys.ESCAPE -> isFocused = false
            Keys.BACKSPACE -> if (selection != caret) deleteSelection() else if (caret > 0) {
                textSetter(text.removeRange(caret - 1, caret))
                caret--
                selection = caret
            }
            Keys.DELETE -> if (selection != caret) deleteSelection() else if (caret < text.length) textSetter(text.removeRange(caret, caret + 1))
            Keys.LEFT -> { if (caret > 0) caret--; if (!shift) selection = caret }
            Keys.RIGHT -> { if (caret < text.length) caret++; if (!shift) selection = caret }
            Keys.A -> if (ctrl) { selection = 0; caret = text.length }
            Keys.C -> if (ctrl && selection != caret) {
                val start = minOf(caret, selection)
                val end = maxOf(caret, selection)
                kh.clipboard = text.substring(start, end)
            }
            Keys.X -> if (ctrl && selection != caret) {
                val start = minOf(caret, selection)
                val end = maxOf(caret, selection)
                kh.clipboard = text.substring(start, end)
                deleteSelection()
            }
            Keys.V -> if (ctrl) { // Ctrl + V (Paste)
                val content = kh.clipboard
                    .replace("\n", "")
                    .replace("\r", "")
                insert(content)
            }
            Keys.Z -> if (ctrl) undo()
        }
        updateCaretPosition()
        return true
    }

    fun updateCaretPosition() {
        val curText = text
        if (curText != cachedPosText || caret != cachedPosCaret || selection != cachedPosSelection
            || fontSize != cachedPosFontSize || font !== cachedPosFont) {
            cachedPosText = curText
            cachedPosCaret = caret
            cachedPosSelection = selection
            cachedPosFontSize = fontSize
            cachedPosFont = font
            caretX = nvg.textWidth(curText.substring(0, caret), fontSize, font)
            val anchorX = nvg.textWidth(curText.substring(0, selection), fontSize, font)
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
        val totalTextWidth = nvg.textWidth(text, fontSize, font)
        val availableWidth = width - (textSidePadding * 2f)
        val centeringOffset = if (centerIfSmall && totalTextWidth < availableWidth) {
            (availableWidth - totalTextWidth) / 2f
        } else 0f

        val localX = mouseX - (absoluteX - textOffset + centeringOffset + textSidePadding)
        caret = (0..text.length).minByOrNull { abs(nvg.textWidth(text.substring(0, it), fontSize, font) - localX) } ?: 0
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

    private fun applyTextInput(active: Boolean) {
        //? if >= 26.3 {
        /*val manager = client.textInputManager()
        if (active) manager.startTextInput(this) else manager.stopTextInput(this)
        *///?}
    }
}