package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.config.core.TextParagraph
import co.stellarskys.stella.utils.config.ui.extentsions.SAWrappedText
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.components.core.Rectangle
import java.awt.Color

class TextParagraphUIBuilder {
    fun build(root: VexelElement<*>, textParagraph: TextParagraph): VexelElement<*> {
        val container = Rectangle(Color(0, 0, 0, 0).rgb)
            .setSizing(100, Size.ParentPerc, 0, Size.Auto)
            .setPositioning(0, Pos.ParentCenter, 0, Pos.AfterSibling)
            .childOf(root)

        val name = SAWrappedText("§f" + textParagraph.name, 14f)
            .setPositioning(0f, Pos.ParentCenter, 5f, Pos.AfterSibling)
            .setSizing(100, Size.ParentPerc, 0f, Size.Auto)
            .childOf(container)

        val description = SAWrappedText("§7" + textParagraph.description, 14f)
            .setPositioning(0f, Pos.ParentCenter, 5f, Pos.AfterSibling)
            .setSizing(100, Size.ParentPerc, 0f, Size.Auto)
            .childOf(container)
        return container
    }
}
