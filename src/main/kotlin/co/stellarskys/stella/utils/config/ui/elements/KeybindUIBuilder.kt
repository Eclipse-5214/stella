package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Keybind
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.extentsions.SAKeybind
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelWindow
import java.awt.Color

class KeybindUIBuilder {
    fun build(root: VexelElement<*>, keybind: Keybind, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.AfterSibling)
            .childOf(root)

        val name = Text(keybind.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window, name, keybind.description)

        val bind = SAKeybind(
            Color.black.rgb,
            Palette.Purple.withAlpha(100).rgb,
            5f
        )
            .setSizing(35f, Size.Pixels, 25f, Size.Pixels)
            .setPositioning(-10f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .alignRight()
            .childOf(container)

        bind.selectedKeyId = (keybind.value as Keybind.Handler).keyCode()

        bind.onValueChange {
            (keybind.value as Keybind.Handler).setCode(it as Int)
        }

        return container
    }
}