package co.stellarskys.stella.features.msc.buttonUtils

import co.stellarskys.stella.api.config.ui.ConfigUI.Companion.UI_SCALE
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.base.BaseElement
import co.stellarskys.stella.api.config.ui.base.TextBox
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import tech.thatgravyboat.skyblockapi.platform.pushPop
import tech.thatgravyboat.skyblockapi.platform.scale
import tech.thatgravyboat.skyblockapi.platform.translate
import java.awt.Color

class EditButtonPopup : BaseElement() {
    var activeAnchor: AnchorType? = null
    var activeIndex: Int = 0
    var shown = false
        private set


    init {
        width = 560f
        height = 640f
        visible = false
        x = (rez.windowWidth / UI_SCALE) / 2f - width / 2f
        y = (rez.windowHeight / UI_SCALE) / 2f - height / 2f
    }

    private val itemIdInput = TextBox(
        40f, 320f, 480f, 70f, "",
        borderColor = Palette.Purple.rgb,
        fontSize = 36f,
        onType = { }
    ).apply { parent = this@EditButtonPopup; maxLength = 64 }

    private val commandInput = TextBox(
        40f, 410f, 480f, 70f, "",
        borderColor = Palette.Purple.rgb,
        fontSize = 36f,
        onType = { }
    ).apply { parent = this@EditButtonPopup; maxLength = 128 }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible) return

        nvg.push()
        nvg.translate(x, y)

        nvg.rect(0f, 0f, width, height, 0xEE000000.toInt(), 20f)
        nvg.hollowRect(0f, 0f, width, height, 3f, Palette.Purple.rgb, 20f)

        val title = "Edit Button"
        nvg.text(title, (width - nvg.textWidth(title, 48f, nvg.inter)) / 2f, 40f, 48f, Color.WHITE.rgb, nvg.inter)

        val previewSize = 160f
        val previewX = (width - previewSize) / 2f
        val previewY = 110f
        nvg.rect(previewX, previewY, previewSize, previewSize, 0xFF111111.toInt(), 16f)
        nvg.hollowRect(previewX, previewY, previewSize, previewSize, 2f, Palette.Purple.rgb, 16f)

        itemIdInput.render(context, mouseX, mouseY, delta)
        commandInput.render(context, mouseX, mouseY, delta)

        drawSimpleButton("Save", 40f, 520f, 230f, 60f, Palette.Green.rgb, mouseX, mouseY)
        drawSimpleButton("Delete", 290f, 520f, 230f, 60f, Palette.Red.rgb, mouseX, mouseY)

        val isXHovered = isAreaHovered(width - 60f, 20f, 40f, 40f, mouseX, mouseY)
        nvg.text("X", width - 50f, 30f, 40f, if (isXHovered) Palette.Red.rgb else Color.WHITE.rgb, nvg.inter)

        nvg.pop()

        val stack = ButtonManager.getItem(itemIdInput.currentText)
        val centerX = x + previewX + (previewSize / 2f)
        val centerY = y + previewY + (previewSize / 2f)
        val mcScale = rez.scaleFactor.toFloat()
        val finalX = (centerX * UI_SCALE * nvg.dpr) / mcScale
        val finalY = (centerY * UI_SCALE * nvg.dpr) / mcScale
        val targetSizeInContext = (previewSize * UI_SCALE * nvg.dpr) / mcScale
        val itemScale = targetSizeInContext / 16f
        context.pushPop {
            context.pose().translate(finalX, finalY)
            context.pose().scale(itemScale, itemScale)
            if(itemIdInput.currentText != "NONE") Render2D.renderItem(context, stack, -8f, -8f, 1f)
        }
    }

    private fun drawSimpleButton(label: String, bx: Float, by: Float, bw: Float, bh: Float, color: Int, mx: Float, my: Float) {
        val hovered = isAreaHovered(bx, by, bw, bh, mx, my)
        nvg.rect(bx, by, bw, bh, if (hovered) color else color and 0x99FFFFFF.toInt(), 12f)
        val tw = nvg.textWidth(label, 36f, nvg.inter)
        nvg.text(label, bx + (bw - tw) / 2f, by + (bh / 2f) - 18f, 36f, Color.WHITE.rgb, nvg.inter)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible) return false
        itemIdInput.mouseClicked(mouseX, mouseY, button)
        commandInput.mouseClicked(mouseX, mouseY, button)

        if (isAreaHovered(width - 60f, 20f, 40f, 40f, mouseX, mouseY)) close()
        if (isAreaHovered(40f, 520f, 230f, 60f, mouseX, mouseY)) save()
        if (isAreaHovered(290f, 520f, 230f, 60f, mouseX, mouseY)) delete()

        return true
    }

    override fun mouseReleased(mouseX: Float, mouseY: Float, button: Int) {
        itemIdInput.mouseReleased(mouseX, mouseY, button)
        commandInput.mouseReleased(mouseX, mouseY, button)
    }

    override fun charTyped(char: Char) = itemIdInput.charTyped(char) || commandInput.charTyped(char)
    override fun keyPressed(keyCode: Int, modifiers: Int) = itemIdInput.keyPressed(keyCode, modifiers) || commandInput.keyPressed(keyCode, modifiers)

    fun open(anchor: AnchorType, index: Int) {
        activeAnchor = anchor
        activeIndex = index
        val existing = ButtonManager.getButtonAt(anchor, index)

        itemIdInput.setText(existing?.iconId ?: "")
        commandInput.setText(existing?.command ?: "")

        visible = true
        shown = true
    }

    fun close() {
        visible = false
        shown = false
        itemIdInput.isFocused = false
        commandInput.isFocused = false
    }

    fun save() {
        val anchor = activeAnchor ?: return
        val id = itemIdInput.currentText
        val cmd = commandInput.currentText

        if (id.isEmpty() || cmd.isEmpty()) {
            ButtonManager.remove(anchor, activeIndex)
        } else {
            val background = !anchor.name.contains("PLAYER")
            val existing = ButtonManager.getButtonAt(anchor, activeIndex)
            if (existing != null) {
                existing.iconId = id
                existing.command = cmd
            } else {
                ButtonManager.add(StellaButton(id, cmd, anchor, activeIndex, background))
            }
        }
        close()
    }

    fun delete() {
        activeAnchor?.let { ButtonManager.remove(it, activeIndex) }
        close()
    }
}