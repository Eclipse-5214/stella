package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.config.ui.Palette
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class Panel(initX: Float, initY: Float, val title: String = ""): BaseElement() {
    val elements = mutableListOf<BaseElement>()

    init {
        x = initX
        y = initY
        width = 100f
        height = 20f
    }

    fun update() {
        height = elements.fold(0f) { acc, e -> acc + e.height }
    }

    override fun render(
        context: GuiGraphics,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        nvg.push()
        nvg.translate(x, y)
        nvg.hollowRect(0f, 0f, width, 20f, 2f, Palette.Purple.rgb, 0f)
        nvg.rect(0f, 0f, width, 20f, Color.BLACK.rgb)

        val tw = nvg.textWidth(title, 16f, nvg.montserrat)
        val tx = width / 2 - tw / 2

        nvg.text(title, tx, 10f, 16f, Color.WHITE.rgb, nvg.montserrat)

        elements.forEach {
            it.render(context, mouseX, mouseY, delta)
        }
        nvg.pop()
    }
}