package co.stellarskys.stella.utils.render

import  co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.utils.clearCodes

object Render2D {
    fun String.width(): Int {
        val lines = split('\n')
        return lines.maxOf { mc.textRenderer.getWidth(it.clearCodes()) }
    }

    fun String.height(): Int {
        val lineCount = count { it == '\n' } + 1
        return mc.textRenderer.fontHeight * lineCount
    }
}