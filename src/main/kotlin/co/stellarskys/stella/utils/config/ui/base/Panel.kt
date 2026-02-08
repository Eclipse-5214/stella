package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.render.nvg.Gradient
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class Panel(initX: Float, initY: Float, val title: String = ""): ParentElement() {
    private var scrollOffset by Utils.animate<Float>(0.25, AnimType.EASE_OUT)

    init {
        x = initX
        y = initY
        height = 25f
        scrollOffset = 0f
    }

    override val absoluteY: Float get() = super.absoluteY + scrollOffset

    override fun render(
        context: GuiGraphics,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        if (isAnimating) update()

        nvg.push()

        nvg.translate(x, y)
        nvg.rect(0f, 0f, width, 20f, Palette.Crust.rgb, 5f, true)

        val tw = nvg.textWidth(title, 10f, nvg.inter)
        val tx = width / 2 - tw / 2

        nvg.text(title, tx, 5f, 10f, Palette.Text.rgb, nvg.inter)
        nvg.pushScissor(0f, 20f, width, height - 20f)
        nvg.translate(0f, scrollOffset)

        elements.forEach {
            it.render(context, mouseX, mouseY, delta)
        }

        val bodyHeight = getEH()

        nvg.push()

        nvg.translate(0f, bodyHeight + 20f)
        nvg.rect(0f, 0f, width, 5f, Palette.Crust.rgb, 5f, false)

        nvg.pop()

        nvg.popScissor()

        nvg.hollowGradientRect(0f, 0f, width, bodyHeight + 25 + scrollOffset, 1f, Palette.Purple.rgb, Palette.Mauve.rgb, Gradient.TopLeftToBottomRight, 5f)

        nvg.pop()
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float, horizontalAmount: Float): Boolean {
        // Only scroll if mouse is inside panel
        if (!isAreaHovered(0f, 0f, width, height)) return false

        val maxScroll = getEH().coerceAtLeast(0f)
        scrollOffset = (scrollOffset + amount * 15f).coerceIn(-maxScroll, 0f)

        return true
    }

    override fun update() {
        height = getEH() + 25f
        updateElements(20f)
    }
}