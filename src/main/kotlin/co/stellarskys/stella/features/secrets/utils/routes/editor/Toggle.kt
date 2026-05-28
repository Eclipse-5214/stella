package co.stellarskys.stella.features.secrets.utils.routes.editor

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.horizon.nvg.BaseElement
import co.stellarskys.stella.utils.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor

class Toggle(initial: Boolean = true) : BaseElement() {
    var value = initial
        private set
    private var anim by Utils.animate<Float>(0.15)

    init { anim = if (initial) 1f else 0f }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        nvg.push()
        nvg.translate(x, y)
        nvg.rect(0f, 0f, width, height, if (value) Palette.Purple.rgb else Palette.Surface1.rgb, height / 2f)
        nvg.rect(2f + (width - height) * anim, 2f, height - 4f, height - 4f, Palette.Text.rgb, (height - 4f) / 2f)
        nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isAreaHovered(0f, 0f, width, height)) return false
        value = !value; anim = if (value) 1f else 0f; return true
    }
}
