package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.core.ColorPicker
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelWindow
import java.awt.Color

class ColorPickerUIBuilder {
    fun build(root: VexelElement<*>, colorpicker: ColorPicker, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.AfterSibling)
            .childOf(root)

        val name = Text(colorpicker.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window, name, colorpicker.description)

        val picker = co.stellarskys.vexel.elements.ColorPicker((colorpicker.value as RGBA).toColor(), Color.BLACK.rgb, Palette.Purple.rgb, 25f, 0f)
            .setSizing(25f, Size.Pixels, 25f, Size.Pixels)
            .setPositioning(-10f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .childOf(container)

        picker.onValueChange { colorpicker.value = RGBA.fromColor(it as Color) }

        return container
    }
}