package co.stellarskys.stella.api.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.api.config.core.Toggle
import co.stellarskys.stella.api.config.ui.ConfigUI
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.config.ui.base.ConfigBase
import co.stellarskys.stella.api.horizon.nvg.BaseElement
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class ToggleUI(initX: Float, initY: Float, val toggle: Toggle): ConfigBase(initX, initY, toggle) {
    private var trackColor by Utils.animate<Color>(0.2, AnimType.EASE_OUT)
    private var thumbColor by Utils.animate<Color>(0.2, AnimType.EASE_OUT)
    private var thumbX by Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private val value get() = toggle.value as Boolean

    init {
        trackColor = if (value) Palette.Purple else Palette.Crust
        thumbColor = if (value) Color.WHITE else Palette.Purple.withAlpha(100)
        thumbX = if (value) 22f else 2f
    }

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        nvg.translate(width - 60f, 14f,)
        nvg.rect(0f, 0f, 42f, HEIGHT - 28, trackColor.rgb, (HEIGHT - 28) / 2)
        nvg.rect(thumbX, 2f, 18f, 18f, thumbColor.rgb, 9f)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.canReceiveInput  == false || !visible) return false
        if (!isAreaHovered(width - 60f, 14f, 42f, HEIGHT - 28)) return false
        toggle.value = !value

        trackColor = if (value) Palette.Purple else Palette.Crust
        thumbColor = if (value) Color.WHITE else Palette.Purple.withAlpha(100)
        thumbX = if (value)  22f else 2f
        return true
    }
}