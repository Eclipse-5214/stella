package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.config.core.ConfigElement
import co.stellarskys.stella.utils.config.ui.ConfigUI
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.render.nvg.Gradient
import net.minecraft.client.gui.GuiGraphics

class Tooltip: BaseElement() {
    init {
        x = (rez.windowWidth / ConfigUI.UI_SCALE) / 2
        y = (rez.windowHeight / ConfigUI.UI_SCALE) - 150f
    }

    var text = ""
        set(value) {
            field = value
            visible = value.isNotEmpty()
        }

    private var padding = 10f

    fun show(element: ConfigElement) {
        text = element.description
    }

    fun hide(element: ConfigElement) {
        if (text != element.description) return
        text = ""
    }

    override fun render(
        context: GuiGraphics,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        if (text.isEmpty() || !visible) return
        val textWidth = nvg.textWidth(text, 16f, nvg.inter)
        nvg.push()
        nvg.translate(x - textWidth / 2f, y)
        nvg.rect(-padding / 2f, -padding / 2f , textWidth + padding, 16f + padding, Palette.Crust.rgb, 8f)
        nvg.text(text, 0f, 0f, 16f, Palette.Text.rgb, nvg.inter)
        nvg.hollowGradientRect(-padding / 2f, -padding / 2f , textWidth + padding, 16f + padding, 2f, Palette.Purple.rgb, Palette.Mauve.rgb, Gradient.TopLeftToBottomRight, 8f)
        nvg.pop()
    }
}