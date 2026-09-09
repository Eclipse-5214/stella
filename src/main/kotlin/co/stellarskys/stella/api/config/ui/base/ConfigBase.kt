package co.stellarskys.stella.api.config.ui.base

import co.stellarskys.stella.api.config.core.ConfigElement
import co.stellarskys.stella.api.config.ui.ConfigUI
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.nvg.BaseElement
import co.stellarskys.stella.utils.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class ConfigBase(initX: Float, initY: Float, val element: ConfigElement, initH: Float = 50f): BaseElement() {
    internal var offsetAnim = Utils.animate<Float>(0.15)
    internal var offset by offsetAnim
    internal var HEIGHT = initH

    init {
        offset = if (visible) 0f else HEIGHT
        x = initX
        y = initY
    }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return

        if (isAnimating) {
            height = HEIGHT - offset
            if (offsetAnim.done()) isAnimating = false
        }

        nvg.pushPop {
            renderBase(context, mouseX, mouseY, delta)
        }
    }

    fun renderBase(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        if (isTextHovered(element.name,12f, 17f)) ConfigUI.tooltip.show(element)
        else ConfigUI.tooltip.hide(element)

        nvg.translate(x, y)
        nvg.pushScissor(0f, 0f, width, HEIGHT - offset)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(element.name, 12f, 17f, 16f, Palette.Text.rgb, nvg.inter)

        onRender(context, mouseX, mouseY, delta)
        nvg.popScissor()
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)

        if (value) {
            offset = 0f
            isAnimating = true
        } else {
            offset = HEIGHT
            isAnimating = true
        }
    }

    open fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {}
}