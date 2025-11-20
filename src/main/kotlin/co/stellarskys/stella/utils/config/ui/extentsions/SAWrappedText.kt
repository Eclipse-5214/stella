package co.stellarskys.stella.utils.config.ui.extentsions

import co.stellarskys.stella.utils.clearCodes
import xyz.meowing.vexel.components.base.Pos
import xyz.meowing.vexel.components.base.Size
import xyz.meowing.vexel.components.base.VexelElement
import xyz.meowing.vexel.utils.render.NVGRenderer
import xyz.meowing.vexel.utils.style.Font
import java.awt.Color

class SAWrappedText(val text: String, val fontSize: Float = 12f): VexelElement<SAWrappedText>() {
    init {
        setSizing(180f, Size.Pixels, 0f, Size.Auto)
        setPositioning(Pos.ParentPixels, Pos.ParentPixels)
    }

    override fun onRender(mouseX: Float, mouseY: Float) {
        drawMinecraftString(
            text,
            x + 6f,
            y + 2f,
            fontSize,
            width - 12f,
            centered = true
        )
    }

    override fun getAutoHeight(): Float = textHeight(text, fontSize, width - 12f) + 2f

    object MCColor {
        private val COLORS = mapOf(
            '0' to Color(0, 0, 0),
            '1' to Color(0, 0, 170),
            '2' to Color(0, 170, 0),
            '3' to Color(0, 170, 170),
            '4' to Color(170, 0, 0),
            '5' to Color(170, 0, 170),
            '6' to Color(255, 170, 0),
            '7' to Color(170, 170, 170),
            '8' to Color(85, 85, 85),
            '9' to Color(85, 85, 255),
            'a' to Color(85, 255, 85),
            'b' to Color(85, 255, 255),
            'c' to Color(255, 85, 85),
            'd' to Color(255, 85, 255),
            'e' to Color(255, 255, 85),
            'f' to Color(255, 255, 255)
        )

        fun getColor(code: Char): Int = COLORS[code]?.rgb ?: Color.WHITE.rgb
    }

    fun drawMinecraftString(
        text: String,
        x: Float,
        y: Float,
        fontSize: Float = 12f,
        maxWidth: Float = Float.MAX_VALUE,
        defaultColor: Int = Color.WHITE.rgb,
        font: Font = NVGRenderer.defaultFont,
        centered: Boolean = false
    ) {
        var currentColor = defaultColor
        var cursorY = y
        val lineHeight = fontSize + 2f

        val tokens = Regex("§.|\\s+|[^\\s§]+").findAll(text).map { it.value }.toList()
        val lineWords = mutableListOf<Pair<String, Int>>() // word + color
        var lineWidth = 0f

        fun flushLine() {
            if (lineWords.isEmpty()) return
            var cursorX = x
            if (centered) {
                cursorX = x + (maxWidth - lineWidth) / 2f
            }
            for ((word, color) in lineWords) {
                NVGRenderer.text(word, cursorX, cursorY, fontSize, color, font)
                cursorX += NVGRenderer.textWidth(word, fontSize, font)
            }
            cursorY += lineHeight
            lineWords.clear()
            lineWidth = 0f
        }

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when {
                token.startsWith("§") && token.length == 2 -> {
                    currentColor = MCColor.getColor(token[1])
                    i++
                }
                token == "\n" -> {
                    flushLine()
                    i++
                }
                token.isBlank() -> {
                    val spaceWidth = NVGRenderer.textWidth(token, fontSize, font)
                    if (lineWidth + spaceWidth > maxWidth) {
                        flushLine()
                    } else {
                        lineWords.add(token to currentColor)
                        lineWidth += spaceWidth
                    }
                    i++
                }
                else -> {
                    val wordWidth = NVGRenderer.textWidth(token, fontSize, font)
                    if (lineWidth + wordWidth > maxWidth && lineWords.isNotEmpty()) {
                        flushLine()
                    }
                    lineWords.add(token to currentColor)
                    lineWidth += wordWidth
                    i++
                }
            }
        }
        flushLine()
    }

    fun textHeight(
        text: String,
        fontSize: Float = 12f,
        maxWidth: Float,
        font: Font = NVGRenderer.defaultFont
    ): Float {
        val lineHeight = fontSize + 2f
        var lines = 1
        var cursorX = 0f
        for (word in text.clearCodes().split(Regex("(?<=\\n)|(?=\\n)| "))) {
            when {
                word == "\n" -> { cursorX = 0f; lines++ }
                word.isBlank() -> {
                    val w = NVGRenderer.textWidth(" ", fontSize, font)
                    if (cursorX + w > maxWidth) { cursorX = 0f; lines++ } else cursorX += w
                }
                else -> {
                    val w = NVGRenderer.textWidth(word, fontSize, font)
                    if (cursorX + w > maxWidth && cursorX > 0f) { cursorX = w; lines++ } else cursorX += w
                }
            }
        }
        return lines * lineHeight
    }

}