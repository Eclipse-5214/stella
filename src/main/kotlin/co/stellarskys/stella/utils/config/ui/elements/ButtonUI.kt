package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.Button
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class ButtonUI(initX: Float, initY: Float, val button: Button): BaseElement() {
    private val delegate = Utils.animate<Color>(0.25, AnimType.SPRING)
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var buttonColor by delegate
    private var offset by offsetAnim

    init {
        offset = if (visible) 0f else -HEIGHT
        buttonColor = Palette.Base
        x = initX
        y = initY
    }

    override fun render(
        context: GuiGraphics,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        if (!visible && !isAnimating) return

        if (isAnimating && offsetAnim.done()) {
            isAnimating = false
        }

        nvg.push()
        nvg.translate(x, y)
        nvg.pushScissor(0f,0f, width, HEIGHT - offset)
        nvg.translate(0f, offset)

        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(button.name, 6f, 8.5f, 8f, Palette.Text.rgb, nvg.inter)
        nvg.translate(width - 40f, 4f,)
        nvg.rect(0f, 0f, 32f, HEIGHT - 8, buttonColor.rgb, 5f)

        val sw = nvg.textWidth(button.placeholder, 8f, nvg.inter)
        nvg.text(button.placeholder, 16f - sw / 2, 4.5f, 8f, Palette.Text.rgb, nvg.inter)

        nvg.popScissor()
        nvg.pop()
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)

        if (value) {
            offset = 0f
            isAnimating = true
        } else {
            offset = -HEIGHT
            isAnimating = true
        }
    }

    companion object {
        const val HEIGHT = 25f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.isAnimating == true) return false
        if (!isAreaHovered(width - 40f, 4f, 32f, HEIGHT - 8)) return false
        delegate.pulse(Palette.Purple)
        this.button.onClick?.invoke()
        return true
    }
}
