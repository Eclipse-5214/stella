package co.stellarskys.stella.api.config.ui.base

import co.stellarskys.stella.api.config.core.ConfigElement
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.utils.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor

abstract class ConfigSub(initX: Float, initY: Float, element: ConfigElement, val expansionHeight: Float, initH: Float = 50f): ConfigBase(initX, initY, element, initH) {
    internal var expansionAnim = Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    internal var expansion by expansionAnim

    val opening get() = expansion < 0.5f

    init {
        expansion = 0f
        height = HEIGHT - offset
    }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return

        if (isAnimating) {
            height = (HEIGHT + (expansionHeight * expansion) - offset).coerceAtLeast(0f)
            if (expansionAnim.done() && offsetAnim.done()) isAnimating = false
        }

        nvg.pushPop {
            renderBase(context, mouseX, mouseY, delta)
            if (expansion > 0.01f) {
                nvg.pushScissor(0f, HEIGHT, width, height - HEIGHT)
                onExpand(context, mouseX, mouseY, delta)
                nvg.popScissor()
            }
        }
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        offset = if (value) 0f else (HEIGHT + (expansionHeight) * expansion)
        isAnimating = true
    }

    abstract fun onExpand(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float)

    fun toggleExpansion() {
        expansion = if (opening) 1f else 0f
        isAnimating = true
    }
}