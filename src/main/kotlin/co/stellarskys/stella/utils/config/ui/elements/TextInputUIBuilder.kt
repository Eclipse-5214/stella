package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.core.TextInput
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.components.input.UITextInput
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import java.awt.Color

class TextInputUIBuilder {
    fun build(root: UIComponent, input: TextInput, window: Window): UIComponent {
        val container = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = PixelConstraint(20f)
            }
            .setColor(Color(0, 0, 0, 0))
            .setChildOf(root)

        val name = UIText(input.name)
            .constrain {
                x = PixelConstraint(7f)
                y = CenterConstraint()
            }
            .setChildOf(container)

        attachTooltip(window, name, input.description)

        val inputBlock = createBlock(2f)
            .constrain {
                width = 60.pixels()
                height = 15.pixels()
                x = PixelConstraint(115f)
                y = CenterConstraint()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(container)

        val inputText = UITextInput(input.value as String, true)
            .constrain {
                width = 54.pixels()
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setColor(Color.WHITE)
            .setChildOf(inputBlock)

        inputBlock.onMouseClick {
            inputText.grabWindowFocus()
        }

        inputText.onKeyType { _, _ ->
            inputText as UITextInput
            input.onValueChanged?.invoke(inputText.getText())
        }

        inputText.onFocusLost {
            inputText as UITextInput
            input.value = inputText.getText()
            inputText.setText(input.value as String)
        }

        return container
    }
}
