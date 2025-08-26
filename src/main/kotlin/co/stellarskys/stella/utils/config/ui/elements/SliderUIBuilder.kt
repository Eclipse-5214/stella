package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.core.Slider
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
import java.text.DecimalFormat
import java.awt.Color

class SliderUIBuilder {
    var inputLocked = false

    fun build(root: UIComponent, slider: Slider, window: Window): UIComponent {
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
        val sliderMin = slider.min
        val sliderMax = slider.max

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
            Palette.Purple, Color(0,0,0,0),
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
                x = knob.getLeft().pixels() // center over knob
                y = knob.getTop().pixels() // float above
                width = 30.pixels()
            }
            .setColor(Palette.Purple)
            .attachToWindow(window)

        valueInput.hide(true)

        fun updateSliderPosition(mouseX: Float) {
            val clamped = mouseX.coerceIn(0f, trackWidth)
            val percent = clamped / trackWidth
            val value = sliderMin + (sliderMax - sliderMin) * percent
            slider.value = value

            knob.animate {
                setXAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    PixelConstraint(100f + clamped - 4f)
                )
            }

            gradient.animate {
                setWidthAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    PixelConstraint(clamped - 3f)
                )
            }

            val inputText = DecimalFormat("#.#").format(value)

            valueInput.animate {
                setXAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    PixelConstraint(track.getLeft() + clamped - 0f - inputText.width() / 2)
                )
            }

            valueInput.setY((knob.getTop() - 10).pixels())

            valueInput as UITextInput
            valueInput.setText(inputText)
        }

        track.onMouseClick { event -> if (event.relativeX in -5f..trackWidth + 5f) updateSliderPosition(event.relativeX)}

        track.onMouseDrag { x, y, _ ->
            val withinX = x in -5f..trackWidth + 5f
            val withinY = y in -5f..(track.getHeight().toFloat() + 5f)
            if (withinY && withinX) updateSliderPosition(x)
        }

        valueInput.onMouseClick {
            valueInput.grabWindowFocus()
        }

        knob.onMouseEnter {
            valueInput.setY((knob.getTop() - 10).pixels())

            if (!inputLocked) {
                valueInput.unhide()
            }
        }

        knob.onMouseLeave {
            if (!inputLocked) {
                valueInput.hide()
            }
        }

        knob.onMouseClick { event ->
            if (event.mouseButton != 1) return@onMouseClick

            inputLocked = true
            valueInput.grabWindowFocus()
            valueInput.unhide()
        }

        valueInput.onFocusLost {
            valueInput as UITextInput
            val parsed = valueInput.getText().toFloatOrNull()
            if (parsed != null) {
                val clamped = parsed.coerceIn(sliderMin, sliderMax)
                updateSliderPosition(((clamped - sliderMin) / (sliderMax - sliderMin)) * trackWidth)
                valueInput.setText(DecimalFormat("#.#").format(clamped))
            } else {
                valueInput.setText(DecimalFormat("#.#").format(slider.value))
            }

            inputLocked = false
            valueInput.hide()
        }

        updateSliderPosition(((slider.value as Float - sliderMin) / (sliderMax - sliderMin)) * trackWidth)
        return container
    }
}
