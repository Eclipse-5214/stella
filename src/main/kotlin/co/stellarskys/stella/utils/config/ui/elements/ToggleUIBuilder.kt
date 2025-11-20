package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Config
import co.stellarskys.stella.utils.config.core.Toggle
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelWindow
import xyz.meowing.vexel.elements.Switch
import java.awt.Color

class ToggleUIBuilder {
    fun build(root: VexelElement<*>, toggle: Toggle, config: Config, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0,0,0,0).rgb)
            .setSizing(100, Size.ParentPerc, 40, Size.Pixels)
            .setPositioning(0, Pos.ParentCenter, 0, Pos.AfterSibling)
            .childOf(root)

        val name =  Text(toggle.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7, Pos.ParentPixels, 0, Pos.ParentCenter)
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
            .setPositioning(-10, Pos.ParentPixels, 0, Pos.ParentCenter)
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
