package co.stellarskys.stella.api.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.api.config.core.Dropdown
import co.stellarskys.stella.api.config.ui.ConfigUI
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.config.ui.base.ConfigSub
import co.stellarskys.stella.api.horizon.nvg.BaseElement
import net.minecraft.client.gui.GuiGraphicsExtractor

class DropdownUI(initX: Float, initY: Float, val dropdown: Dropdown) : ConfigSub(initX, initY, dropdown, dropdown.options.size * OPTION_HEIGHT) {
    private var caretRot by Utils.animate<Double>(0.15)
    private var hoveredIndex = -1

    init {
        caretRot = -90.0
    }

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        nvg.rect(BOX_X, BOX_Y, BOX_W, BOX_H, Palette.Base.rgb, 6f)
        nvg.hollowRect(BOX_X, BOX_Y, BOX_W, BOX_H, 2f, Palette.Purple.withAlpha(100).rgb, 6f)

        nvg.pushScissor(BOX_X + 4f, BOX_Y, BOX_W - 28f, BOX_H)
        nvg.text(dropdown.options.getOrNull(dropdown.value as Int) ?: "None", BOX_X + 8f, BOX_Y + 7f, 14f, Palette.Text.rgb, nvg.inter)
        nvg.popScissor()

        nvg.pushPop {
            nvg.translate(BOX_X + BOX_W - 14f, BOX_Y + (BOX_H / 2f))
            nvg.rotate(Math.toRadians(caretRot).toFloat())
            nvg.image(ConfigUI.caretImage, -7f, -7f, 14f, 14f, Palette.Text.rgb)
        }

        hoveredIndex = if (expansion > 0.5f && isAreaHovered(0f, HEIGHT, width, expansionHeight, mouseX, mouseY)) {
            ((mouseY - (absoluteY + HEIGHT)) / OPTION_HEIGHT).toInt().coerceIn(0, dropdown.options.size - 1)
        } else -1
    }

    override fun onExpand(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        nvg.pushScissor(0f, HEIGHT, width, height - HEIGHT)
        nvg.rect(0f, HEIGHT, width, expansionHeight, Palette.Crust.withAlpha(100).rgb)

        dropdown.options.forEachIndexed { i, opt ->
            val optY = HEIGHT + (i * OPTION_HEIGHT)
            if (hoveredIndex == i) nvg.rect(8f, optY + 4f, width - 16f, OPTION_HEIGHT - 8f, Palette.Purple.withAlpha(100).rgb, 8f)
            if (i == dropdown.value as Int) nvg.rect(4f, optY + 8f, 3f, OPTION_HEIGHT - 16f, Palette.Purple.rgb, 2f)
            nvg.text(opt, 24f, optY + 12f, 15f, Palette.Text.rgb, nvg.inter)
        }

        nvg.popScissor()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.canReceiveInput  == false || !visible || offset > 1f || parent?.isAnimating == true) return false
        if (isAreaHovered(BOX_X, BOX_Y, BOX_W, BOX_H, mouseX, mouseY)) {
            toggleExpansion()
            caretRot = if (opening) 0.0 else -90.0
            return true
        }

        if (expansion > 0.8f && hoveredIndex != -1) {
            dropdown.value = hoveredIndex
            toggleExpansion()
            caretRot = -90.0
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    companion object {
        const val OPTION_HEIGHT = 36f
        const val BOX_W = 84f
        const val BOX_H = 28f
        const val BOX_X = 240f - BOX_W - 8f
        const val BOX_Y = (50f - BOX_H) / 2f
    }
}