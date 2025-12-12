package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.StepSlider
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelWindow
import co.stellarskys.vexel.elements.NumberInput
import java.awt.Color
import java.math.RoundingMode


class StepSliderUIBuilder {
    fun build(root: VexelElement<*>, slider: StepSlider, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.AfterSibling)
            .childOf(root)

        val name = Text(slider.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window, name, slider.description)

        val mSlider = co.stellarskys.vexel.elements.Slider(
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
            .setSizing(90f, Size.Pixels, 20f, Size.Pixels)
            .setPositioning(-55f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .childOf(container)

        val numberInput = NumberInput(
            0,
            fontSize = 14f,
            backgroundColor = Color.BLACK.rgb,
            borderColor = Palette.Purple.withAlpha(100).rgb
        )
            .setSizing(35f, Size.Pixels, 25f, Size.Pixels)
            .setPositioning(-10f, Pos.ParentPixels, 0f, Pos.ParentCenter)
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
