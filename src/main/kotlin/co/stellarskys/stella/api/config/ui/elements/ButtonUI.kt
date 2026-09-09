package co.stellarskys.stella.api.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.api.config.core.Button
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.base.ConfigBase
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class ButtonUI(initX: Float, initY: Float, val button: Button): ConfigBase(initX, initY, button) {
    private val delegate = Utils.animate<Color>(0.25, AnimType.SPRING)
    private var buttonColor by delegate

    init { buttonColor = Palette.Base }

    override fun onRender(
        context: GuiGraphicsExtractor,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        nvg.translate(width - 80f, 8f,)
        nvg.rect(0f, 0f, 64f, HEIGHT - 16, buttonColor.rgb, 10f)

        val sw = nvg.textWidth(button.placeholder, 16f, nvg.inter)
        nvg.text(button.placeholder, 32f - sw / 2, 9f, 16f, Palette.Text.rgb, nvg.inter)
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.canReceiveInput == false ||parent?.isAnimating == true || !visible) return false
        if (!isAreaHovered(width - 80f, 8f, 64f, HEIGHT - 16)) return false
        delegate.pulse(Palette.Purple)
        this.button.onClick?.invoke()
        return true
    }
}
