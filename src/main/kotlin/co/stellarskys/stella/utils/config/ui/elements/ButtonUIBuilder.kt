package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config.core.Button
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import net.minecraft.client.gui.GuiGraphics

import java.awt.Color

class ButtonUIBuilder {
    fun build(x: Float, y: Float, button: Button): BaseElement {
        return ButtonUI(x, y, button)
    }

    class ButtonUI(initX: Float, initY: Float, val button: Button): BaseElement() {
        private val delegate = Utils.lerped<Color>(0.15)
        private var buttonColor by delegate
        private var pulsing = false
        private var pulseUp = true

        init {
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
            if (!visible) return
            if (pulsing) {
                if (delegate.done()) {
                    if (pulseUp) pulseUp = false
                    else {
                        pulsing = false
                        pulseUp = true
                    }
                } else {
                    buttonColor = if (pulseUp) Palette.Purple else Palette.Base
                }
            }

            nvg.push()
            nvg.translate(x, y)

            nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
            nvg.text(button.name, 6f, 8.5f, 8f, Palette.Text.rgb, nvg.inter)
            nvg.translate(width - 40f, 4f,)
            nvg.rect(0f, 0f, 32f, HEIGHT - 8, buttonColor.rgb, 5f)

            val sw = nvg.textWidth(button.placeholder, 8f, nvg.inter)
            nvg.text(button.placeholder, 16f - sw / 2, 4.5f, 8f, Palette.Text.rgb, nvg.inter)
            nvg.pop()
        }

        companion object {
            const val HEIGHT = 25f
        }

        override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
            if (!isAreaHovered(width - 40f, 4f, 32f, HEIGHT - 8)) return false
            this.button.onClick?.invoke()
            pulse()
            return true
        }

        fun pulse() {
            pulsing = true
            pulseUp = true
            buttonColor = Palette.Base.brighter() // first target
        }

    }
}