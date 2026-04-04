package co.stellarskys.stella.features.msc.buttonUtils

import co.stellarskys.stella.api.config.ui.ConfigUI.Companion.UI_SCALE
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.stella.api.nvg.NVGRenderer
import co.stellarskys.stella.api.zenith.Aperture
import co.stellarskys.stella.api.zenith.Zenith
import net.minecraft.client.gui.GuiGraphics

class ButtonLayoutEditor : Aperture() {
    private val slotSize = 20
    private val popup = EditButtonPopup()
    private val mouse = Zenith.Mouse
    private val mx get() = mouse.rawX.toFloat() / UI_SCALE
    private val my get() = mouse.rawY.toFloat() / UI_SCALE

    override fun onRender(context: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
        context.fill(0, 0, width, height, 0x90000000.toInt())

        // Draw dummy inventory
        val invX = (width - 176) / 2
        val invY = (height - 166) / 2

        context.drawNVG {
            NVGRenderer.hollowRect(
                invX.toFloat(),
                invY.toFloat(),
                176f,
                166f,
                1f,
                0xFFAAAAAA.toInt(),
                4f
            )

            for (anchor in AnchorType.entries) {
                for (index in 0 until anchor.slots) {
                    val (x, y) = ButtonManager.resolveAnchorPosition(anchor, index, invX, invY)

                    NVGRenderer.hollowRect(
                        x.toFloat(),
                        y.toFloat(),
                        slotSize.toFloat(),
                        slotSize.toFloat(),
                        1f,
                        0xFFAAAAAA.toInt(),
                        4f
                    )

                    ButtonManager.getAll().find { it.anchor == anchor && it.index == index }?.let { button ->
                        if (popup.shown) return@let
                        if (button.iconId == "NONE") return@let
                        val stack = ButtonManager.getItem(button.iconId)

                        val offsetX = (20f - 16f) / 2f
                        val offsetY = (20f - 16f) / 2f

                        Render2D.renderItem(context, stack, x.toFloat() + offsetX, y.toFloat() + offsetY, 1f)
                    }
                }
            }
        }

        context.drawNVG(false) {
            NVGRenderer.push()
            NVGRenderer.scale(UI_SCALE, UI_SCALE)
            popup.render(context, mx, my, tickDelta)
            NVGRenderer.pop()
        }

        super.onRender(context, mouseX, mouseY, tickDelta)
    }

    override fun onKeyPress(key: Int, scanCode: Int, modifiers: Int): Boolean {
        return popup.keyPressed(key, modifiers) || super.onKeyPress(key, scanCode, modifiers)
    }

    override fun onCharTyped(char: Char): Boolean {
        return popup.charTyped(char) || super.onCharTyped(char)
    }

    override fun onMouseClick(button: Int, x: Double, y: Double, modifiers: Int): Boolean {
        val invX = (width - 176) / 2
        val invY = (height - 166) / 2

        if (!popup.shown) {
            for (anchor in AnchorType.entries) {
                for (index in 0 until anchor.slots) {
                    val (bx, by) = ButtonManager.resolveAnchorPosition(anchor, index, invX, invY)
                    if (x.toInt() in bx..(bx + slotSize) && y.toInt() in by..(by + slotSize)) {
                        popup.open(anchor, index)
                        return super.onMouseClick(button, x, y, modifiers)
                    }
                }
            }
        } else {
            popup.mouseClicked(mx, my, button)
            return super.onMouseClick(button, x, y, modifiers)
        }

        return super.onMouseClick(button, x, y, modifiers)
    }

    override fun onMouseRelease(button: Int, x: Double, y: Double, modifiers: Int): Boolean {
        popup.mouseReleased(mx, my, button)
        return super.onMouseRelease(button, x, y, modifiers)
    }

    override fun onBackgroundRender(context: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {}
}