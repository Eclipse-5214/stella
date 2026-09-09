package co.stellarskys.stella.api.config.ui.elements

import co.stellarskys.stella.api.config.core.TextParagraph
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.config.ui.base.ConfigBase
import net.minecraft.client.gui.GuiGraphicsExtractor
import java.awt.Color

class TextParagraphUI(initX: Float, initY: Float, val paragraph: TextParagraph) : ConfigBase(initX, initY, paragraph) {
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
        val titleH = calculateMinecraftHeight("§f${paragraph.name}", 16f, width - 24f)
        val descH = calculateMinecraftHeight("§7${paragraph.description}", 14f, width - 24f)
        HEIGHT = titleH + descH + 24f // Base padding
        height = (HEIGHT - offset).coerceAtLeast(0f)
    }

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return

        if (isAnimating) {
            height = HEIGHT - offset
            if (offsetAnim.done()) isAnimating = false
        }

        nvg.pushPop {
            nvg.translate(x, y)
            nvg.pushScissor(0f, 0f, width, HEIGHT - offset)
            nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)

            val titleHeight = drawMinecraftText("§f${paragraph.name}", 12f, 12f, 16f, width - 24f)
            drawMinecraftText("§7${paragraph.description}", 12f, 12f + titleHeight + 4f, 14f, width - 24f)

            nvg.popScissor()
        }
    }

    private fun drawMinecraftText(text: String, xPos: Float, yPos: Float, size: Float, maxWidth: Float): Float {
        var currentColor = Color.WHITE.rgb
        var cursorY = yPos
        val lineHeight = size + 4f
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
        val lineHeight = size + 4f
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