package co.stellarskys.stella.utils.config.ui.base

import xyz.meowing.vexel.components.core.Text
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.utils.render.NVGRenderer

class WrappedText(
    text: String,
    val color: Int,
    val bounds: Float
) : VexelElement<WrappedText>() {
    init {
        val bounds = NVGRenderer.wrappedTextBounds(text, 228f, 14f, NVGRenderer.defaultFont)
        val textHeight = bounds[3] - bounds[1]
        setSizing(240f, Size.Pixels, textHeight + 16f, Size.Pixels)
        setPositioning(Pos.ParentPixels, Pos.AfterSibling)
        Text(text, color, 12f)
            .setPositioning(6f, Pos.ParentPixels, 8f, Pos.ParentPixels)
            .childOf(this)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {
        children.firstOrNull()?.let { textElement ->
            if (textElement is Text) {
                NVGRenderer.drawWrappedString(
                    textElement.text,
                    x + 6f,
                    y + 8f,
                    bounds,
                    12f,
                    color,
                    NVGRenderer.defaultFont
                )
            }
        }
    }
}