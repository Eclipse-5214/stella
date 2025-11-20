package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Dropdown
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.extentsions.SADropdown
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelWindow
import java.awt.Color

class DropdownUIBuilder {
    fun build(root: VexelElement<*>, dropdown: Dropdown, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100, Size.ParentPerc, 40, Size.Pixels)
            .setPositioning(0, Pos.ParentCenter, 0, Pos.AfterSibling)
            .childOf(root)

        val name = Text(dropdown.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7, Pos.ParentPixels, 0, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window, name, dropdown.description)

        val mDropdown = SADropdown(
            dropdown.options,
            (dropdown.value as Int),
            Color.BLACK.rgb,
            Color.WHITE.rgb,
            Palette.Purple.withAlpha(100).rgb,
            6f,
            2f,
            )
            .setPositioning(-10, Pos.ParentPixels, 0, Pos.ParentCenter)
            .setSizing(90f, Size.Pixels, 25f, Size.Pixels)
            .alignRight()
            .fontSize(14f)
            .childOf(container)

        mDropdown.onValueChange { dropdown.value = it as Int }

        return container
    }
}