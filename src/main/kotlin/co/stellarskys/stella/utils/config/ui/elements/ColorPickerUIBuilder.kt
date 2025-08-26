package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.core.ColorPicker
import co.stellarskys.stella.utils.config.core.FloatingUIManager
import co.stellarskys.stella.utils.config.core.attachToWindow
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import java.awt.Color

class ColorPickerUIBuilder {
    fun build(root: UIComponent, colorpicker: ColorPicker, window: Window): UIComponent {
        val wrapper = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = PixelConstraint(20f)
            }
            .setColor(Color(0,0,0,0))
            .setChildOf(root)

        // config element
        val name = UIText(colorpicker.name)
            .constrain {
                x = PixelConstraint(7f)
                y = CenterConstraint()
            }
            .setChildOf(wrapper)

        attachTooltip(window,name, colorpicker.description)

        val swatch = createBlock(3f)
            .constrain {
                width = 16.pixels()
                height = 16.pixels()
                x = PixelConstraint(159f)
                y = CenterConstraint()
            }
            .setColor((colorpicker.value as RGBA).toColor())
            .setChildOf(wrapper)


        //picker
        val pickerWrapper = UIBlock()
            .constrain {
                width = 78.pixels()
                height = 96.pixels()
                x = (wrapper.getRight() + 10).pixels()
                y = wrapper.getTop().pixels()
            }
            .setColor(Color.black)
            .effect(OutlineEffect(Palette.Purple.withAlpha(100), 1f))
            .setChildOf(window)

        pickerWrapper.hide()

        FloatingUIManager.registerColorPicker(pickerWrapper)

        val textInput = createBlock(4f)
            .constrain {
                x = CenterConstraint()
                y = 4.pixels()
                width = 70.pixels()
                height = 15.pixels()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(pickerWrapper)

        val hexText = UITextInput((colorpicker.value as RGBA).toHex())
            .constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = PixelConstraint(54f)
            }
            .setChildOf(textInput)

        hexText.onMouseClick {
            grabWindowFocus()
        }

        hexText.onKeyType { _, _ ->
            val input = (hexText as UITextInput).getText()
            try {
                val parsed = RGBA.fromHex(input)
                colorpicker.value = parsed
                swatch.setColor(parsed.toColor())
            } catch (_: Exception) {
                // fallback: revert to previous
            }
        }

        hexText.onFocusLost {
            var input = (this as UITextInput).getText()

            // Append # if missing
            if (!input.startsWith("#")) {
                input = "#$input"
            }

            // Try parsing it
            try {
                val parsed = RGBA.fromHex(input)
                setText(parsed.toHex())
            } catch (_: Exception) {
                setText("#ffffffff")
                val default = RGBA(255,255,255)
                colorpicker.value = default
                swatch.setColor(default.toColor())
            }
        }

        // Extract hue from current color
        val current = colorpicker.value as RGBA
        val (hue, sat, value, alpha) = (colorpicker.value as RGBA).toHSVA()

        // Base layer: Solid hue, full sat/val
        val svBase = UIBlock()
            .setColor(Color.getHSBColor(hue, 1f, 1f)) // fixed hue
            .constrain {
                width = 53.pixels()
                height = 53.pixels()
                x = PixelConstraint(3f)
                y = PixelConstraint(24f)
            }
            .setChildOf(pickerWrapper)

        // Overlay: saturation (white → transparent)
        GradientComponent(
            Color.WHITE, Color(255, 255, 255, 0),
            GradientComponent.GradientDirection.LEFT_TO_RIGHT
        ).constrain {
            width = RelativeConstraint(1f)
            height = RelativeConstraint(1f)
            x = CenterConstraint()
            y = CenterConstraint()
        }.setChildOf(svBase)

        // Overlay: value (transparent → black)
        GradientComponent(
            Color(0, 0, 0, 0), Color.BLACK,
            GradientComponent.GradientDirection.TOP_TO_BOTTOM
        ).constrain {
            width = RelativeConstraint(1f)
            height = RelativeConstraint(1f)
            x = CenterConstraint()
            y = CenterConstraint()
        }.setChildOf(svBase)

        // Cursor
        val cursor = CursorOutline(4f,4f,1f)
            .setChildOf(svBase)

        // Update Cursor position
        val boxWidth = svBase.getWidth().toFloat()
        val boxHeight = svBase.getHeight().toFloat()

        val cursorX = (sat * boxWidth) - cursor.getWidth() / 2f
        val cursorY = ((1f - value) * boxHeight) - cursor.getHeight() / 2f

        cursor.setX(PixelConstraint(cursorX))
        cursor.setY(PixelConstraint(cursorY))

        // Update SV on drag
        svBase.onMouseDrag { mouseX, mouseY, _ ->
            if (mouseX !in 0f..svBase.getWidth().toFloat() || mouseY !in 0f..svBase.getHeight().toFloat()) return@onMouseDrag

            updateColorFromSVInput(mouseX, mouseY, svBase, colorpicker, hexText, swatch, cursor)
        }

        // Update SV on click
        svBase.onMouseClick { event ->
            val mouseX = event.relativeX
            val mouseY = event.relativeY

            updateColorFromSVInput(mouseX, mouseY, svBase, colorpicker, hexText, swatch, cursor)
        }

        // Hue bar
        val hueBarHeight = 53f
        val sliceCount = 53 // more = smoother gradient
        val sliceHeight = hueBarHeight / sliceCount

        val hueBar = UIBlock()
            .constrain {
                width = 12.pixels()
                height = PixelConstraint(hueBarHeight)
                x = PixelConstraint(62f)
                y = PixelConstraint(24f)
            }
            .setColor(Color(0, 0, 0, 0)) // transparent base
            .setChildOf(pickerWrapper)

        for (i in 0 until sliceCount) {
            val hue = i / sliceCount.toFloat()
            val color = Color.getHSBColor(hue, 1f, 1f)

            UIBlock()
                .constrain {
                    width = RelativeConstraint(1f)
                    height = PixelConstraint(sliceHeight)
                    x = CenterConstraint()
                    y = PixelConstraint(i * sliceHeight)
                }
                .setColor(color)
                .setChildOf(hueBar)
        }

        // Cursor
        val hbCursor = CursorOutline(14f,4f,1f)
            .setChildOf(hueBar)

        // Set its position
        val hbCursorX = hueBar.getWidth().toFloat() / 2f - hbCursor.getWidth() / 2f
        val hbCursorY = (hue * hueBar.getHeight().toFloat()) - hbCursor.getHeight() / 2f

        hbCursor.setX(PixelConstraint(hbCursorX))
        hbCursor.setY(PixelConstraint(hbCursorY))

        // Update Hue on drag
        hueBar.onMouseDrag { mouseX, mouseY, _ ->
            val hbh = hueBar.getHeight()
            if (mouseX !in 0f..hueBar.getWidth().toFloat() || mouseY !in 0f..hbh) return@onMouseDrag

            updateColorFromHueInput(mouseY,hbh, colorpicker,svBase, hexText, swatch,hbCursor)
        }

        // Update Hue on click
        hueBar.onMouseClick { event ->
            val mouseY = event.relativeY
            val hbh = hueBar.getHeight()

            updateColorFromHueInput(mouseY,hbh, colorpicker,svBase, hexText, swatch,hbCursor)
        }

        // Alpha Slider
        val alphaBar = UIBlock()
            .constrain {
                width = 53.pixels()
                height = 9.pixels()
                x = PixelConstraint(3f)
                y = PixelConstraint(82f)
            }
            .setColor(Palette.Purple)
            .setChildOf(pickerWrapper)

        // Cursor
        val abCursor = CursorOutline(4f,11f,1f)
            .setChildOf(alphaBar)

        // Set its position
        val abCursorX = (alpha * alphaBar.getWidth().toFloat()) - abCursor.getWidth() / 2f
        val abCursorY = alphaBar.getHeight().toFloat() / 2f - abCursor.getHeight() / 2f

        abCursor.setX(PixelConstraint(abCursorX))
        abCursor.setY(PixelConstraint(abCursorY))

        val alphaInputWrapper = UIRoundedRectangle(5f)
            .constrain {
                width = 15.pixels()
                height = 15.pixels()
                x = PixelConstraint(60f)
                y = PixelConstraint(79f)
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(pickerWrapper)

        val formattedAlpha = String.format("%.1f", alpha)

        val alphaText = UITextInput(formattedAlpha)
            .constrain {
                x = CenterConstraint()
                y = CenterConstraint()
                width = PixelConstraint(14f)
            }
            .setChildOf(alphaInputWrapper)

        alphaText.onMouseClick {
            grabWindowFocus()
        }

        alphaText.onKeyType { _, _ ->
            val raw = (alphaText as UITextInput).getText()
            val typed = raw.toFloatOrNull()
            if (typed != null && typed in 0f..1f) {
                val newColor = RGBA.fromHSVA(hue, sat, value, typed)
                colorpicker.value = newColor
                swatch.setColor(newColor.toColor())
                //(hexText as UITextInput).setText(newColor.toHex())
            }
        }

        alphaText.onFocusLost {
            val cleaned = (alphaText as UITextInput).getText().toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
            val formatted = String.format("%.1f", cleaned)
            alphaText.setText(formatted)

            val newColor = RGBA.fromHSVA(hue, sat, value, cleaned)

            colorpicker.value = newColor
            swatch.setColor(newColor.toColor())
            //(hexText as UITextInput).setText(newColor.toHex())
        }

        // Update Hue on drag
        alphaBar.onMouseDrag { mouseX, mouseY, _ ->
            val abw = alphaBar.getWidth()
            if (mouseX !in 0f..abw || mouseY !in 0f..alphaBar.getHeight().toFloat()) return@onMouseDrag

            updateColorFromAlphaInput(mouseX, abw, colorpicker, swatch, hexText, alphaText, abCursor)
        }

        // Update Hue on click
        alphaBar.onMouseClick { event ->
            val mouseX = event.relativeX
            val abw = alphaBar.getWidth()

            updateColorFromAlphaInput(mouseX, abw, colorpicker, swatch, hexText, alphaText, abCursor)
        }

        // logic
        swatch.onMouseClick {
            pickerWrapper.setY(wrapper.getTop().pixels())

            if (FloatingUIManager.getActivePicker() != pickerWrapper) {
                FloatingUIManager.setActivePicker(pickerWrapper)
            } else {
                pickerWrapper.hide()
                FloatingUIManager.clearActivePicker()
            }
        }
        return wrapper
    }

    private fun updateColorFromSVInput(
        mouseX: Float,
        mouseY: Float,
        svBox: UIComponent,
        colorpicker: ColorPicker,
        hexText: UIComponent,
        swatch: UIComponent,
        cursor: UIComponent
    ) {
        val boxWidth = svBox.getWidth().toFloat()
        val boxHeight = svBox.getHeight().toFloat()

        val relX = (mouseX / boxWidth).coerceIn(0f, 1f) // Saturation
        val relY = (mouseY / boxHeight).coerceIn(0f, 1f) // Value (inverted)

        val (hue, _, _, alpha) = (colorpicker.value as RGBA).toHSVA()
        val newColor = RGBA.fromHSVA(hue, relX, 1f - relY, alpha)

        colorpicker.value = newColor
        swatch.setColor(newColor.toColor())
        (hexText as UITextInput).setText(newColor.toHex())

        // Move the cursor (center it based on its own size)
        val cursorX = (relX * boxWidth) - cursor.getWidth() / 2f
        val cursorY = (relY * boxHeight) - cursor.getHeight() / 2f

        cursor.setX(PixelConstraint(cursorX))
        cursor.setY(PixelConstraint(cursorY))
    }

    fun updateColorFromHueInput(
        mouseY: Float,
        hueBarHeight: Float,
        colorpicker: ColorPicker,
        svBox: UIComponent,
        hexText: UIComponent,
        swatch: UIComponent,
        hueCursor: UIComponent,
    ): Float {
        val relY = (mouseY / hueBarHeight).coerceIn(0f, 1f)
        val newHue = relY // allow manual hue override if needed

        // Keep current sat/value/alpha
        val (_, sat, value, alpha) = (colorpicker.value as RGBA).toHSVA()
        val updatedColor = RGBA.fromHSVA(newHue, sat, value, alpha)

        colorpicker.value = updatedColor
        swatch.setColor(updatedColor.toColor())
        (hexText as UITextInput).setText(updatedColor.toHex())

        // Recolor the SV gradient base (should be done externally if needed)
        svBox.setColor(Color.getHSBColor(newHue, 1f, 1f))

        // Move the hue cursor to match the new hue
        val cursorY = (newHue * hueBarHeight) - hueCursor.getHeight() / 2f
        hueCursor.setY(PixelConstraint(cursorY))

        return newHue
    }

    fun updateColorFromAlphaInput(
        mouseX: Float,
        alphaBarWidth: Float,
        colorpicker: ColorPicker,
        swatch: UIComponent,
        hexText: UIComponent,
        alphaText: UIComponent,
        alphaCursor: UIComponent
    ): Float {
        val relX = (mouseX / alphaBarWidth).coerceIn(0f, 1f)
        val newAlpha = relX

        val (hue, sat, value, _) = (colorpicker.value as RGBA).toHSVA()

        val newColor = RGBA.fromHSVA(hue, sat, value, newAlpha)
        colorpicker.value = newColor
        swatch.setColor(newColor.toColor())
        (hexText as UITextInput).setText(newColor.toHex())

        // Update the alpha text box too
        (alphaText as UITextInput).setText(String.format("%.1f", newAlpha))

        // Update the cursor
        val cursorX = relX * alphaBarWidth - alphaCursor.getWidth() / 2f
        alphaCursor.setX(PixelConstraint(cursorX))

        return newAlpha
    }

    fun CursorOutline(cwidth: Float, cheight: Float, thickness: Float): UIComponent {
        val wrapper = UIBlock()
            .constrain {
                width = cwidth.pixels()
                height = cheight.pixels()
                x = PixelConstraint(0f)
                y = PixelConstraint(0f)
            }
            .setColor(Color(0, 0, 0, 0)) // transparent center

        // Top
        UIBlock().constrain {
            x = 0.pixels(); y = 0.pixels()
            width = cwidth.pixels(); height = thickness.pixels()
        }.setColor(Color.WHITE).setChildOf(wrapper)

        // Bottom
        UIBlock().constrain {
            x = 0.pixels(); y = PixelConstraint(cheight - thickness)
            width = cwidth.pixels(); height = thickness.pixels()
        }.setColor(Color.WHITE).setChildOf(wrapper)

        // Left
        UIBlock().constrain {
            x = 0.pixels(); y = 0.pixels()
            width = thickness.pixels(); height = cheight.pixels()
        }.setColor(Color.WHITE).setChildOf(wrapper)

        // Right
        UIBlock().constrain {
            x = PixelConstraint(cwidth - thickness); y = 0.pixels()
            width = thickness.pixels(); height = cheight.pixels()
        }.setColor(Color.WHITE).setChildOf(wrapper)

        return wrapper
    }
}