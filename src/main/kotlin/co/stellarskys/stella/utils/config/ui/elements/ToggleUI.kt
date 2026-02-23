package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.Toggle
import co.stellarskys.stella.utils.config.ui.ConfigUI
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
        thumbX = if (value) 22f else 2f
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

        if (isTextHovered(toggle.name,12f, 17f)) ConfigUI.tooltip.show(toggle)
        else ConfigUI.tooltip.hide(toggle)

        nvg.push()
        nvg.translate(x, y)
        nvg.pushScissor(0f,0f, width, HEIGHT - offset)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(toggle.name, 12f, 17f, 16f, Palette.Text.rgb, nvg.inter)
        nvg.translate(width - 60f, 14f,)
        nvg.rect(0f, 0f, 42f, HEIGHT - 28, trackColor.rgb, (HEIGHT - 28) / 2)
        nvg.rect(thumbX, 2f, 18f, 18f, thumbColor.rgb, 9f)

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
        if (parent?.canReceiveInput  == false || !visible) return false
        if (!isAreaHovered(width - 60f, 14f, 42f, HEIGHT - 28)) return false
        toggle.value = !value

        trackColor = if (value) Palette.Purple else Palette.Crust
        thumbColor = if (value) Color.WHITE else Palette.Purple.withAlpha(100)
        thumbX = if (value)  22f else 2f
        return true
    }

    companion object {
        const val HEIGHT = 50f
    }
}