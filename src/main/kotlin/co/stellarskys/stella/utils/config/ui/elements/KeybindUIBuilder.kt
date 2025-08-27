package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.core.Keybind
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
//#if MC >= 1.21.5
import org.lwjgl.glfw.GLFW
//#elseif MC == 1.8.9
//$$ import org.lwjgl.input.Keyboard
//#endif
import java.awt.Color


class KeybindUIBuilder {
    fun build(root: UIComponent, keybind: Keybind, window: Window): UIComponent {
        var listening = false

        val container = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = PixelConstraint(20f)
            }
            .setColor(Color(0, 0, 0, 0))
            .setChildOf(root)

        val name = UIText(keybind.name)
            .constrain {
                x = PixelConstraint(7f)
                y = CenterConstraint()
            }
            .setChildOf(container)

        attachTooltip(window, name, keybind.description)

        val keybindInput = createBlock(2f)
            .constrain {
                width = 40.pixels()
                height = 15.pixels()
                x = PixelConstraint(135f)
                y = CenterConstraint()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(container)

        val keyDisplay = UIText(getKeyName(keybind.value as Int))
            .constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setColor(Color.WHITE)
            .setChildOf(keybindInput)

        keybindInput.onMouseClick {
            grabWindowFocus()
            listening = true
            keyDisplay as UIText
            keyDisplay.setText("...")
            keyDisplay.setColor(Palette.Mauve)
        }

        keybindInput.onKeyType { _, keycode ->
            if (listening) {
                keyDisplay as UIText
                keyDisplay.setText(getKeyName(keycode))
                keyDisplay.setColor(Color.WHITE)
                keybind.value = keycode
                listening = false
            }
        }

        return container
    }

    private fun getKeyName(keyCode: Int): String = when (keyCode) {
        340 -> "LShift"
        344 -> "RShift"
        341 -> "LCtrl"
        345 -> "RCtrl"
        342 -> "LAlt"
        346 -> "RAlt"
        257 -> "Enter"
        256 -> "Escape"
        in 290..301 -> "F${keyCode - 289}" // F1–F12
        //#if MC >= 1.21.5
        else -> GLFW.glfwGetKeyName(keyCode, 0) ?: "Key$keyCode"
        //#elseif MC == 1.8.9
        //$$ else -> Keyboard.getKeyName(keyCode) ?: "Key$keyCode"
        //#endif
    }
}
