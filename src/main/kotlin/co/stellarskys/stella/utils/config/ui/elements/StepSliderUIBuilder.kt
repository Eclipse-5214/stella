package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.StepSlider
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelWindow
import xyz.meowing.vexel.elements.NumberInput
import java.awt.Color
import java.math.RoundingMode


class StepSliderUIBuilder {
    fun build(root: VexelElement<*>, slider: StepSlider, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100, Size.ParentPerc, 40, Size.Pixels)
            .setPositioning(0, Pos.ParentCenter, 0, Pos.AfterSibling)
            .childOf(root)

        val name = Text(slider.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7, Pos.ParentPixels, 0, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window, name, slider.description)

        val mSlider = xyz.meowing.vexel.elements.Slider(
            (slider.value as Int).toFloat(),
            slider.min.toFloat(),
            slider.max.toFloat(),
            slider.step.toFloat(),
            thumbColor = Color.WHITE.rgb,
            trackColor = Palette.Purple.withAlpha(100).rgb,
            trackFillColor = Palette.Purple.withAlpha(200).rgb,
            thumbWidth = 15f,
            thumbHeight = 15f
        )
            .setSizing(90, Size.Pixels, 20, Size.Pixels)
            .setPositioning(-55, Pos.ParentPixels, 0, Pos.ParentCenter)
            .alignRight()
            .childOf(container)

        val numberInput = NumberInput(
            0,
            fontSize = 14f,
            backgroundColor = Color.BLACK.rgb,
            borderColor = Palette.Purple.withAlpha(100).rgb
        )
            .setSizing(35f, Size.Pixels, 25f, Size.Pixels)
            .setPositioning(-10, Pos.ParentPixels, 0, Pos.ParentCenter)
            .alignRight()
            .childOf(container)

        numberInput.stringValue = (slider.value as Int).toString()

        mSlider.onValueChange {
            val value = (it as Float).toBigDecimal().setScale(2, RoundingMode.HALF_EVEN).toInt()

            if (!numberInput.isFocused) numberInput.stringValue = value.toString()
            slider.value = value
        }

        numberInput.onValueChange {
            val value = ((it as String).toIntOrNull() ?: 0).coerceIn(slider.min, slider.max)

            if(!mSlider.isDragging) mSlider.setValue(value.toFloat())
            slider.value = value
        }

        return container
    }
}
