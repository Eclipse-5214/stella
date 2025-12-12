package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Config
import co.stellarskys.stella.utils.config.core.Toggle
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelWindow
import co.stellarskys.vexel.elements.Switch
import java.awt.Color

class ToggleUIBuilder {
    fun build(root: VexelElement<*>, toggle: Toggle, config: Config, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0,0,0,0).rgb)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.AfterSibling)
            .childOf(root)

        val name =  Text(toggle.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window, name, toggle.description)

        val toggleSwitch = Switch(
            Color.WHITE.rgb,
            Palette.Purple.rgb,
            Palette.Purple.rgb,
            Palette.Purple.withAlpha(100).rgb,
            borderThickness = 0f
        )

        toggleSwitch.enabledPadding = -6f

        toggleSwitch
            .setPositioning(-10f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .setEnabled(toggle.value as Boolean)
            .alignRight()
            .childOf(container)

        toggleSwitch.onValueChange {
            toggle.value = it as Boolean
            config.notifyListeners(toggle.configName, toggle.value)
        }

        return container
    }
}
