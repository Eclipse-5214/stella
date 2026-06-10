package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.mc.BaseElement
import co.stellarskys.stella.api.zenith.client
import net.minecraft.client.gui.GuiGraphics

class SearchBar(
    initX: Float,
    initY: Float,
    initWidth: Float,
    initHeight: Float,
    private val onUpdate: () -> Unit
) : BaseElement() {
    var query = ""
        private set
    var isFocused = false
    private var selectAll = false

    init {
        x = initX
        y = initY
        width = initWidth
        height = initHeight
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        ren2d.drawRect(context, x.toInt(), y.toInt(), width.toInt(), height.toInt(), Palette.Crust.withAlpha(150))
        ren2d.drawHollowRect(context, x.toInt(), y.toInt(), width.toInt(), height.toInt(), 1, if (isFocused) Palette.Green else Palette.Purple)
        
        if (query.isEmpty() && !isFocused) {
            ren2d.drawString(context, "§7Search...", x.toInt() + 4, y.toInt() + 3, 1f)
        } else {
            if (selectAll && isFocused && query.isNotEmpty()) {
                ren2d.drawRect(context, x.toInt() + 3, y.toInt() + 2, client.font.width(query) + 1, height.toInt() - 4, Palette.Blue.withAlpha(100))
            }
            ren2d.drawString(context, query + if (isFocused && !selectAll && (System.currentTimeMillis() / 500) % 2 == 0L) "_" else "", x.toInt() + 4, y.toInt() + 3, 1f)
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isAreaHovered(0f, 0f, width, height, mouseX, mouseY)) {
            isFocused = true
            selectAll = false
            return true
        } else if (isFocused) {
            isFocused = false
            selectAll = false
        }
        return false
    }

    override fun charTyped(char: Char): Boolean {
        if (isFocused) {
            if (selectAll) {
                query = ""
                selectAll = false
            }
            if (client.font.width(query + char) < width - 10) {
                query += char
                onUpdate()
            }
            return true
        }
        return false
    }

    override fun keyPressed(keyCode: Int, modifiers: Int): Boolean {
        val ctrl = (modifiers and 2) != 0
        if (ctrl && keyCode == 70) { // ctrl + f
            isFocused = true
            selectAll = true
            return true
        }
        if (isFocused) {
            if (keyCode == 259 && query.isNotEmpty()) { // backspace
                query = if (ctrl || selectAll) "" else query.dropLast(1)
                selectAll = false
                onUpdate()
            } else if (keyCode == 256) { // Escape
                isFocused = false
                selectAll = false
            } else if (ctrl && keyCode == 65) { // ctrl + a
                selectAll = true
            } else if (ctrl && keyCode == 86) { // ctrl + v
                if (selectAll) query = ""
                val clipboard = net.minecraft.client.Minecraft.getInstance().keyboardHandler.clipboard
                if (client.font.width(query + clipboard) < width - 10) {
                    query += clipboard
                    onUpdate()
                }
                selectAll = false
            } else if (ctrl && keyCode == 67) { // ctrl + c
                if (selectAll) net.minecraft.client.Minecraft.getInstance().keyboardHandler.clipboard = query
            } else if (ctrl && keyCode == 88) { // ctrl + x
                if (selectAll) {
                    net.minecraft.client.Minecraft.getInstance().keyboardHandler.clipboard = query
                    query = ""
                    selectAll = false
                    onUpdate()
                }
            } else if (keyCode == 263 || keyCode == 262) {  // left and right arrows
                selectAll = false
            }
            return true
        }
        return false
    }
}
