package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Button
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.core.VexelWindow

import java.awt.Color

class ButtonUIBuilder {
    fun build(root: VexelElement<*>, button: Button, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0,0,0,0).rgb)
            .setSizing(100f, Size.Percent, 40f, Size.Pixels)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.AfterSibling)
            .childOf(root)

        val name =  Text(button.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window,name, button.description)

        val buttonInput = co.stellarskys.vexel.elements.Button(
            button.placeholder,
            Color.WHITE.rgb,
            Color.WHITE.rgb,
            Color.WHITE.rgb,
            14f,
            shadowEnabled = false,
            borderRadius = 5f,
            borderThickness = 0f,
            backgroundColor = Palette.Purple.withAlpha(100).rgb,
            hoverColor = Palette.Purple.withAlpha(150).rgb,
            pressedColor = Palette.Purple.rgb
            )
            .setPositioning(-10f, Pos.ParentPixels, 0f, Pos.ParentCenter)
            .setSizing(50f, Size.Pixels, 25f, Size.Pixels)
            .alignRight()
            .childOf(container)

        buttonInput.onMouseClick { _ ->
            button.onClick?.invoke()
            true
        }

        return container
    }
}