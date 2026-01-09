package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class Panel(initX: Float, initY: Float, val title: String = ""): BaseElement() {
    val elements = mutableListOf<BaseElement>()

    init {
        x = initX
        y = initY
        height = 25f
    }

    fun update() {
        height = getEH() + 25
    }

    fun getEH() = elements.fold(0f) { acc, e -> acc + e.height }

    override fun render(
        context: GuiGraphics,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        nvg.push()
        nvg.translate(x, y)
        nvg.rect(0f, 0f, width, 20f, Palette.Crust.rgb, 5f, true)

        val tw = nvg.textWidth(title, 10f, nvg.inter)
        val tx = width / 2 - tw / 2

        nvg.text(title, tx, 5f, 10f, Palette.Text.rgb, nvg.inter)

        elements.forEach {
            it.render(context, mouseX, mouseY, delta)
        }

        val bodyHeight = getEH()
        nvg.push()
        nvg.translate(0f, bodyHeight + 20f)
        nvg.rect(0f, 0f, width, 5f, Palette.Crust.rgb, 5f, false)
        nvg.pop()
        nvg.hollowRect(0f, 0f, width, bodyHeight + 25, 1f, Palette.Purple.rgb, 5f)
        nvg.pop()
    }
}