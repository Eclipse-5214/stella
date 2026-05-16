package co.stellarskys.stella.features.secrets.utils.routes.editor

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.horizon.nvg.BaseElement
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class Button(
    private val label: String,
    private val baseColor: Color,
    private val onClick: () -> Unit
) : BaseElement() {
    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        val hovered = isAreaHovered(0f, 0f, width, height)
        nvg.push()
        nvg.translate(x, y)
        nvg.rect(0f, 0f, width, height, (if (hovered) baseColor.brighter() else baseColor).rgb, 8f)
        val tw = nvg.textWidth(label, 14f, nvg.inter)
        nvg.text(label, width / 2f - tw / 2f, height / 2f - 7f, 14f, Palette.Text.rgb, nvg.inter)
        nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isAreaHovered(0f, 0f, width, height)) return false
        onClick(); return true
    }
}
