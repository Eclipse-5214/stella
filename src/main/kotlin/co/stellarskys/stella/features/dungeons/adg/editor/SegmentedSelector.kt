package co.stellarskys.stella.features.dungeons.adg.editor

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.nvg.BaseElement
import net.minecraft.client.gui.GuiGraphicsExtractor

class SegmentedSelector<T>(
    private val options: List<T>,
    initial: T,
    private val display: (T) -> String,
) : BaseElement() {
    var value: T = initial
        private set

    private val segmentWidth get() = width / options.size

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        nvg.push()
        nvg.translate(x, y)

        nvg.rect(0f, 0f, width, height, Palette.Base.rgb, 8f)
        nvg.hollowRect(0f, 0f, width, height, 1f, Palette.Purple.withAlpha(50).rgb, 8f)

        options.forEachIndexed { index, option ->
            val segmentX = index * segmentWidth
            val selected = option == value

            when {
                selected -> nvg.rect(segmentX + 2f, 2f, segmentWidth - 4f, height - 4f, Palette.Purple.rgb, 6f)
                isAreaHovered(segmentX, 0f, segmentWidth, height) ->
                    nvg.rect(segmentX + 2f, 2f, segmentWidth - 4f, height - 4f, Palette.Surface0.rgb, 6f)
            }

            val label = display(option)
            val textWidth = nvg.textWidth(label, 13f, nvg.inter)
            val textColor = if (selected) Palette.Text.rgb else Palette.Subtext0.rgb
            nvg.text(label, segmentX + segmentWidth / 2f - textWidth / 2f, height / 2f - 6.5f, 13f, textColor, nvg.inter)
        }

        nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isAreaHovered(0f, 0f, width, height)) return false
        val index = ((mouseX - absoluteX) / segmentWidth).toInt().coerceIn(options.indices)
        value = options[index]
        return true
    }
}
