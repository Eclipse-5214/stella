package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.TextParagraph
import co.stellarskys.stella.utils.config.ui.Palette
import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.*
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import java.awt.Color

class TextParagraphUIBuilder {
    fun build(root: UIComponent, textParagraph: TextParagraph): UIComponent {
        val container = UIBlock()
            .constrain {
                width = 180.pixels()
                height = 40.pixels()
                x = CenterConstraint()
                y = PixelConstraint(20f)
            }
            .setColor(Color(0, 0, 0, 0))
            .setChildOf(root)

        val title = UIText(textParagraph.name)
            .constrain {
                x = CenterConstraint()
                y = PixelConstraint(6f)
            }
            .setTextScale(1.1f.pixels())
            .setColor(Color.WHITE)
            .setChildOf(container)

        val description = UIWrappedText("§7" + textParagraph.description, centered = true)
            .constrain {
                x = CenterConstraint()
                y = PixelConstraint(16f)
                width = 170.pixels()
            }
            .setChildOf(container)

        return container
    }
}
