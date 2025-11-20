package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.RGBA
import co.stellarskys.stella.utils.config.core.ColorPicker
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelWindow
import java.awt.Color

class ColorPickerUIBuilder {
    fun build(root: VexelElement<*>, colorpicker: ColorPicker, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100, Size.ParentPerc, 40, Size.Pixels)
            .setPositioning(0, Pos.ParentCenter, 0, Pos.AfterSibling)
            .childOf(root)

        val name = Text(colorpicker.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7, Pos.ParentPixels, 0, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window, name, colorpicker.description)

        val picker = xyz.meowing.vexel.elements.ColorPicker((colorpicker.value as RGBA).toColor(), Color.BLACK.rgb, Palette.Purple.rgb, 25f, 0f)
            .setSizing(25f, Size.Pixels, 25f, Size.Pixels)
            .setPositioning(-10, Pos.ParentPixels, 0, Pos.ParentCenter)
            .alignRight()
            .childOf(container)

        picker.onValueChange { colorpicker.value = RGBA.fromColor(it as Color) }

        return container
    }
}