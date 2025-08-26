package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.core.Dropdown
import co.stellarskys.stella.utils.config.core.attachToWindow
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.OutlineEffect
import java.awt.Color

class DropdownUIBuilder {
    var panelHidden = true

    fun build(root: UIComponent, dropdown: Dropdown, window: Window): UIComponent {
        val wrapper = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = PixelConstraint(20f)
            }
            .setColor(Color(0, 0, 0, 0))
            .setChildOf(root)

        val name = UIText(dropdown.name)
            .constrain {
                x = PixelConstraint(7f)
                y = CenterConstraint()
            }
            .setChildOf(wrapper)

        attachTooltip(window, name, dropdown.description)

        val selectedLabel = (dropdown.options.getOrNull(dropdown.value as Int) ?: "Select")

        val dropdownButton = createBlock(2f)
            .constrain {
                width = 60.pixels()
                height = 15.pixels()
                x = PixelConstraint(105f)
                y = CenterConstraint()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(wrapper)

        val carrotThing = createBlock(2f)
            .constrain {
                width = 10.pixels()
                height = 15.pixels()
                x = 166.pixels()
                y = CenterConstraint()
            }
            .setColor(Palette.Purple.withAlpha(100))
            .setChildOf(wrapper)

        val label = UIText(selectedLabel)
            .constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(dropdownButton)

        val carrot = UIText("▼")
            .constrain {
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(carrotThing)

        // Create dropdown panel once and toggle visibility
        val dropdownPanel = UIBlock()
            .constrain {
                width = 78.pixels()
                height = if (dropdown.options.size <= 4) (dropdown.options.size * 18 + 2).pixels() else 74.pixels()
                x = (wrapper.getRight() - 78).pixels()
                y = wrapper.getBottom().pixels()
            }
            .setColor(Color.black)
            .effect(OutlineEffect(Palette.Purple.withAlpha(100), 1f))
            .attachToWindow(window)

        dropdownPanel.hide()

        val scroller = ScrollComponent()
            .constrain {
                width = RelativeConstraint(1f)
                height = RelativeConstraint(1f)
                x = CenterConstraint()
                y = CenterConstraint()
            }
            .setChildOf(dropdownPanel)

        dropdown.options.forEachIndexed { index, option ->
            val entryContainer = createBlock(4f)
                .constrain {
                    width = 74.pixels()
                    height = 16.pixels()
                    x = CenterConstraint()
                    y = if (index == 0) PixelConstraint(2f) else SiblingConstraint(2f)
                }
                .setColor(Palette.Purple.withAlpha(100))
                .setChildOf(scroller)

            val entryText = UIText(option)
                .constrain {
                    x = CenterConstraint()
                    y = CenterConstraint()
                }
                .setColor(Color.WHITE)
                .setChildOf(entryContainer)

            entryContainer.onMouseClick {
                dropdown.value = index
                label as UIText
                label.setText(option)
                carrot as UIText
                carrot.setText("▼")
                dropdownPanel.hide()
            }
        }

        dropdownButton.onMouseClick { updateDropdown(dropdownPanel, carrot, wrapper) }
        carrotThing.onMouseClick { updateDropdown(dropdownPanel, carrot, wrapper) }

        return wrapper


    }

    fun updateDropdown(dropdownPanel: UIComponent, carrot: UIComponent, wrapper: UIComponent) {
        if (panelHidden) {
            dropdownPanel
                .setY(wrapper.getBottom().pixels())
                .unhide()

            carrot as UIText
            carrot.setText("▲")

            panelHidden = false
        } else {
            dropdownPanel.hide()
            panelHidden = true

            carrot as UIText
            carrot.setText("▼")
        }
    }
}