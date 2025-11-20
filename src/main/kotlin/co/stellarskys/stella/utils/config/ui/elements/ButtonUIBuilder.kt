package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Button
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.core.attachTooltip
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.core.VexelWindow

import java.awt.Color

class ButtonUIBuilder {
    fun build(root: VexelElement<*>, button: Button, window: VexelWindow): VexelElement<*> {
        val container = Rectangle(Color(0,0,0,0).rgb)
            .setSizing(100, Size.ParentPerc, 40, Size.Pixels)
            .setPositioning(0, Pos.ParentCenter, 0, Pos.AfterSibling)
            .childOf(root)

        val name =  Text(button.name, shadowEnabled = false, fontSize = 14f)
            .setPositioning(7, Pos.ParentPixels, 0, Pos.ParentCenter)
            .childOf(container)

        attachTooltip(window,name, button.description)

        val buttonInput = xyz.meowing.vexel.elements.Button(
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
            .setPositioning(-10, Pos.ParentPixels, 0, Pos.ParentCenter)
            .setSizing(50f, Size.Pixels, 25f, Size.Pixels)
            .alignRight()
            .childOf(container)

        buttonInput.onMouseClick { _, _, _ ->
            button.onClick?.invoke()
            true
        }

        return container
    }
}