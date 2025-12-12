package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.TextParagraph
import co.stellarskys.stella.utils.config.ui.extentsions.SAWrappedText
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.components.core.Rectangle
import java.awt.Color

class TextParagraphUIBuilder {
    fun build(root: VexelElement<*>, textParagraph: TextParagraph): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100f, Size.Percent, 0f, Size.Auto)
            .setPositioning(0f, Pos.ParentCenter, 0f, Pos.AfterSibling)
            .childOf(root)

        val name = SAWrappedText("§f" + textParagraph.name, 14f)
            .setPositioning(0f, Pos.ParentCenter, 5f, Pos.AfterSibling)
            .setSizing(100f, Size.Percent, 0f, Size.Auto)
            .childOf(container)

        val description = SAWrappedText("§7" + textParagraph.description, 14f)
            .setPositioning(0f, Pos.ParentCenter, 5f, Pos.AfterSibling)
            .setSizing(100f, Size.Percent, 0f, Size.Auto)
            .childOf(container)
        return container
    }
}
