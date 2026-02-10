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
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var trackColor by Utils.animate<Color>(0.2, AnimType.EASE_OUT)
    private var thumbColor by Utils.animate<Color>(0.2, AnimType.EASE_OUT)
    private var thumbX by Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var offset by offsetAnim
    private val value get() = toggle.value as Boolean

    init {
        trackColor = if (value) Palette.Purple else Palette.Crust
        thumbColor = if (value) Color.WHITE else Palette.Purple.withAlpha(100)
        thumbX = if (value) 11f else 1f
        offset = if (visible) 0f else HEIGHT
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
        nvg.pushScissor(0f,0f, width, HEIGHT - offset)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(toggle.name, 6f, 8.5f, 8f, Palette.Text.rgb, nvg.inter)
        nvg.translate(width - 30f, 7f,)
        nvg.rect(0f, 0f, 21f, HEIGHT - 14, trackColor.rgb, (HEIGHT - 14) / 2)
        nvg.rect(thumbX, 1f, 9f, 9f, thumbColor.rgb, 4.5f)

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

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible) return false
        if (!isAreaHovered(width - 30f, 7f, 21f, HEIGHT - 14)) return false
        toggle.value = !value

        trackColor = if (value) Palette.Purple else Palette.Crust
        thumbColor = if (value) Color.WHITE else Palette.Purple.withAlpha(100)
        thumbX = if (value)  11f else 1f
        return true
    }

    companion object {
        const val HEIGHT = 25f
    }
}