package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.core.StepSlider
import co.stellarskys.stella.utils.config.core.attachToWindow
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.render.Render2D.width
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color


class StepSliderUIBuilder {
    var inputLocked = false

    fun build(root: UIComponent, slider: StepSlider, window: Window): UIComponent {
        val container = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = PixelConstraint(20f)
            }
            .setColor(Color(0, 0, 0, 0))
            .setChildOf(root)

        val name = UIText(slider.name)
            .constrain {
                x = PixelConstraint(7f)
                y = CenterConstraint()
            }
            .setChildOf(container)

        attachTooltip(window, name, slider.description)

        val trackWidth = 70f
        val min = slider.min
        val max = slider.max
        val step = slider.step
        val range = max - min

        val track = UIBlock()
            .constrain {
                width = trackWidth.pixels()
                height = 5.pixels()
                x = PixelConstraint(100f)
                y = CenterConstraint()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(container)

        val gradient = GradientComponent(
            Palette.Purple, Color(0, 0, 0, 0),
            GradientComponent.GradientDirection.LEFT_TO_RIGHT
        )
            .constrain {
                width = 0.pixels()
                height = 5.pixels()
                x = PixelConstraint(100f)
                y = CenterConstraint()
            }
            .setChildOf(container)

        val knob = createBlock(3f)
            .constrain {
                width = 8.pixels()
                height = 8.pixels()
                x = PixelConstraint(100f)
                y = CenterConstraint()
            }
            .setColor(Palette.Purple)
            .setChildOf(container)

        val valueInput = UITextInput("", true)
            .constrain {
                x = knob.getLeft().pixels()
                y = knob.getTop().pixels()
                width = 30.pixels()
            }
            .setColor(Palette.Purple)
            .attachToWindow(window)

        valueInput.hide(true)

        fun quantize(value: Int): Int =
            ((value - min + step / 2) / step) * step + min

        fun updateSliderPositionFromStep(value: Int) {
            val clamped = value.coerceIn(min, max)
            val percent = (clamped - min).toFloat() / range.toFloat()
            val knobX = percent * trackWidth

            slider.value = clamped

            knob.animate {
                setXAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    PixelConstraint(100f + knobX - 4f)
                )
            }

            gradient.animate {
                setWidthAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    PixelConstraint(knobX - 3f)
                )
            }

            val inputText = clamped.toString()

            valueInput.animate {
                setXAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    PixelConstraint(track.getLeft() + knobX - inputText.width() / 2)
                )
            }

            valueInput.setY((knob.getTop() - 10).pixels())

            valueInput as UITextInput
            valueInput.setText(inputText)
        }

        fun updateSliderPositionFromMouse(mouseX: Float) {
            val clamped = mouseX.coerceIn(0f, trackWidth)
            val percent = clamped / trackWidth
            val rawValue = (min + (range * percent)).toInt()
            updateSliderPositionFromStep(quantize(rawValue))
        }

        track.onMouseClick { event -> if (event.relativeX in -5f..trackWidth + 5f) updateSliderPositionFromMouse(event.relativeX) }

        track.onMouseDrag { x, y, _ ->
            val withinX = x in -5f..trackWidth + 5f
            val withinY = y in -5f..(track.getHeight().toFloat() + 5f)
            if (withinY && withinX) updateSliderPositionFromMouse(x)
        }

        valueInput.onMouseClick {
            valueInput.grabWindowFocus()
        }

        knob.onMouseEnter {
            valueInput.setY((knob.getTop() - 10).pixels())
            if (!inputLocked) valueInput.unhide()
        }

        knob.onMouseLeave {
            if (!inputLocked) valueInput.hide()
        }

        knob.onMouseClick { event ->
            if (event.mouseButton != 1) return@onMouseClick
            inputLocked = true
            valueInput.grabWindowFocus()
            valueInput.unhide()
        }

        valueInput.onFocusLost {
            valueInput as UITextInput
            val parsed = valueInput.getText().toIntOrNull()
            if (parsed != null) {
                updateSliderPositionFromStep(quantize(parsed))
            } else {
                valueInput.setText(slider.value.toString())
            }

            inputLocked = false
            valueInput.hide()
        }

        updateSliderPositionFromStep(slider.value as Int)

        return container
    }
}
