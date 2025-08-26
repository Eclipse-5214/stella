package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.Config
import co.stellarskys.stella.utils.config.core.Toggle
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.Utils.createBlock
import co.stellarskys.stella.utils.config.core.attachTooltip
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.animation.Animations
import gg.essential.elementa.dsl.*
import java.awt.Color

/**
 * Responsible for constructing the visual toggle component bound to a config boolean value.
 *
 * Includes a label, description text, and a switch with an animated knob to reflect the current state.
 * The toggle updates its internal value on click and triggers a refresh lambda to recompute visibility logic.
 */

class ToggleUIBuilder {

    /**
     * Builds a toggle UI component bound to the given Toggle element.
     *
     * The toggle includes a label, description, and a switch with an animated knob.
     * Clicking the switch updates the Toggle's value and invokes the provided refresh callback.
     *
     * @param root The UIComponent to attach the toggle container to.
     * @param toggle The Toggle data model representing the option.
     * @return A fully constructed UIComponent representing the toggle.
     */

    fun build(root: UIComponent, toggle: Toggle, config: Config, window: Window): UIComponent {
        val toggleContainer = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 20.pixels()
                x = CenterConstraint()
                y = 0.pixels()
            }
            .setColor(Color(0,0,0,0))
            .setChildOf(root)

        val toggleName = UIText(toggle.name, false)
            .constrain {
                x = PixelConstraint(7f) // Moves text to the left
                y = CenterConstraint()
            }
            .setChildOf(toggleContainer)

        attachTooltip(window, toggleName, toggle.description)

        val toggleSwitch = createBlock(5f)
            .constrain {
                width = 30.pixels()
                height = 15.pixels()
                x = PixelConstraint(145f)  // Positions it on the right
                y = CenterConstraint()
            }
            .setColor(if (toggle.value as Boolean) Palette.Purple else Palette.Purple.withAlpha(100))
            .setChildOf(toggleContainer)

        val toggleKnob = createBlock(5f)
            .constrain {
                width = 11.pixels()
                height = 11.pixels()
                x = PixelConstraint(if (toggle.value as Boolean) 17f else 2f) // Moves knob left/right
                y = CenterConstraint()
            }
            .setColor(if (toggle.value as Boolean) Color.white else Color(100,100,100,100))
            .setChildOf(toggleSwitch)

        toggleSwitch.onMouseClick {
            toggle.value = !(toggle.value as Boolean) // Flip toggle state

            toggleKnob.animate {
                setXAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    PixelConstraint(if (toggle.value as Boolean) 17f else 2f) // Moves knob accordingly
                )

                setColorAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    (if (toggle.value as Boolean) Color.white else Color(100,100,100,100)).toConstraint()
                )
            }

            this.animate {
                setColorAnimation(
                    Animations.OUT_CUBIC,
                    0.2f,
                    (if (toggle.value as Boolean) Palette.Purple else Palette.Purple.withAlpha(100)).toConstraint()
                )
            }

            config.notifyListeners(toggle.configName, toggle.value)
        }

        return toggleContainer
    }
}
