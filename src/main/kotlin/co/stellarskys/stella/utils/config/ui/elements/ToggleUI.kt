package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.Toggle
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class ToggleUI(initX: Float, initY: Float, val toggle: Toggle): BaseElement() {
    var trackColor by Utils.animate<Color>(0.2, AnimType.EASE_OUT)
    var thumbColor by Utils.animate<Color>(0.2, AnimType.EASE_OUT)
    var thumbX by Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    val value get() = toggle.value as Boolean

    init {
        trackColor = if (value) Palette.Purple else Palette.Crust
        thumbColor = if (value) Color.WHITE else Palette.Purple.withAlpha(100)
        thumbX = if (value) 17f else 2f
        x = initX
        y = initY
    }

    override fun render(
        context: GuiGraphics,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        if (!visible) return

        nvg.push()
        nvg.translate(x, y)

        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(toggle.name, 6f, 8.5f, 8f, Palette.Text.rgb, nvg.inter)
        nvg.translate(width - 40f, 4f,)
        nvg.rect(0f, 0f, 32f, HEIGHT - 8, trackColor.rgb, (HEIGHT - 8) / 2)
        nvg.rect(thumbX, 2f, 13f, 13f, thumbColor.rgb, 6.5f)

        nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (parent?.isAnimating == true) return false
        if (!isAreaHovered(width - 40f, 4f, 32f, HEIGHT - 8)) return false
        toggle.value = !value

        // animate track color
        trackColor = if (value) Palette.Purple else Palette.Crust
        // animate thumb position
        thumbColor = if (value) Color.WHITE else Palette.Purple.withAlpha(100)
        thumbX = if (value)  17f else 2f
        return true
    }

    companion object {
        const val HEIGHT = 25f
    }
}