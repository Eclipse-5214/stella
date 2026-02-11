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
        offset = if (visible) 0f else HEIGHT
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

        if (isAnimating) {
            height = HEIGHT - offset
            if (offsetAnim.done()) isAnimating = false
        }

        nvg.push()
        nvg.translate(x, y)
        nvg.pushScissor(0f, 0f, width, HEIGHT - offset)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(button.name, 12f, 17f, 16f, Palette.Text.rgb, nvg.inter)
        nvg.translate(width - 80f, 8f,)
        nvg.rect(0f, 0f, 64f, HEIGHT - 16, buttonColor.rgb, 10f)

        val sw = nvg.textWidth(button.placeholder, 16f, nvg.inter)
        nvg.text(button.placeholder, 32f - sw / 2, 9f, 16f, Palette.Text.rgb, nvg.inter)

        nvg.popScissor()
        nvg.pop()
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

    companion object {
        const val HEIGHT = 50f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.canReceiveInput == false ||parent?.isAnimating == true || !visible) return false
        if (!isAreaHovered(width - 80f, 8f, 64f, HEIGHT - 16)) return false
        delegate.pulse(Palette.Purple)
        this.button.onClick?.invoke()
        return true
    }
}
