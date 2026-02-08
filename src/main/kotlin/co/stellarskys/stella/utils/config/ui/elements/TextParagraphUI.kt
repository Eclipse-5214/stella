package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config.core.TextParagraph
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class TextParagraphUI(initX: Float, initY: Float, val paragraph: TextParagraph) : BaseElement() {
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var offset by offsetAnim
    private var calculatedHeight = 0f

    private val colorMap = mapOf(
        '0' to Color(0, 0, 0).rgb,
        '1' to Color(0, 0, 170).rgb,
        '2' to Color(0, 170, 0).rgb,
        '3' to Color(0, 170, 170).rgb,
        '4' to Color(170, 0, 0).rgb,
        '5' to Color(170, 0, 170).rgb,
        '6' to Color(255, 170, 0).rgb,
        '7' to Color(170, 170, 170).rgb,
        '8' to Color(85, 85, 85).rgb,
        '9' to Color(85, 85, 255).rgb,
        'a' to Color(85, 255, 85).rgb,
        'b' to Color(85, 255, 255).rgb,
        'c' to Color(255, 85, 85).rgb,
        'd' to Color(255, 85, 255).rgb,
        'e' to Color(255, 255, 85).rgb,
        'f' to Color(255, 255, 255).rgb
    )

    init {
        x = initX; y = initY; width = 120f

        val titleH = calculateMinecraftHeight("§f${paragraph.name}", 8f, width - 12f)
        val descH = calculateMinecraftHeight("§7${paragraph.description}", 7f, width - 12f)
        calculatedHeight = titleH + descH + 12f // Base padding

        offset = if (visible) 0f else calculatedHeight
        height = (calculatedHeight - offset).coerceAtLeast(0f)
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        offset = if (value) 0f else calculatedHeight
        isAnimating = true
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return

        if (isAnimating) {
            height = (calculatedHeight - offset).coerceAtLeast(0f)
            if (offsetAnim.done()) isAnimating = false
        }

        nvg.push(); nvg.translate(x, y); nvg.pushScissor(0f, 0f, width, height)
        nvg.rect(0f, 0f, width, calculatedHeight, Palette.Crust.withAlpha(150).rgb)

        // Render Title
        val titleHeight = drawMinecraftText("§f${paragraph.name}", 6f, 6f, 8f, width - 12f)

        // Render Description starting after the title
        drawMinecraftText("§7${paragraph.description}", 6f, 6f + titleHeight + 2f, 7f, width - 12f)

        nvg.popScissor(); nvg.pop()
    }

    private fun drawMinecraftText(text: String, xPos: Float, yPos: Float, size: Float, maxWidth: Float): Float {
        var currentColor = Color.WHITE.rgb
        var cursorY = yPos
        val lineHeight = size + 2f
        val tokens = Regex("§.|\\s+|[^\\s§]+").findAll(text).map { it.value }.toList()
        val lineWords = mutableListOf<Pair<String, Int>>()
        var lineWidth = 0f

        fun flushLine() {
            if (lineWords.isEmpty()) return
            var cursorX = xPos
            for ((word, color) in lineWords) {
                nvg.text(word, cursorX, cursorY, size, color, nvg.inter)
                cursorX += nvg.textWidth(word, size, nvg.inter)
            }
            cursorY += lineHeight
            lineWords.clear()
            lineWidth = 0f
        }

        for (token in tokens) {
            when {
                token.startsWith("§") && token.length == 2 -> {
                    currentColor = colorMap[token[1]] ?: currentColor
                }
                token == "\n" -> flushLine()
                else -> {
                    val wordWidth = nvg.textWidth(token, size, nvg.inter)
                    if (lineWidth + wordWidth > maxWidth && lineWords.isNotEmpty()) {
                        flushLine()
                    }
                    lineWords.add(token to currentColor)
                    lineWidth += wordWidth
                }
            }
        }
        flushLine()
        return cursorY - yPos
    }

    private fun calculateMinecraftHeight(text: String, size: Float, maxWidth: Float): Float {
        val lineHeight = size + 2f
        var lines = 1
        var cursorX = 0f
        val clean = text.replace(Regex("§."), "")
        for (word in clean.split(Regex("(?<=\\n)|(?=\\n)| "))) {
            val w = nvg.textWidth(word, size, nvg.inter)
            if (cursorX + w > maxWidth && cursorX > 0f) {
                cursorX = w
                lines++
            } else {
                cursorX += w
            }
        }
        return lines * lineHeight
    }
}