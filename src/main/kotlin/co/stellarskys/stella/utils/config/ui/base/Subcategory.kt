package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.config.core.ConfigSubcategory
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color

class Subcategory(initX: Float, initY: Float, val subcategory: ConfigSubcategory): BaseElement() {
    val elements = mutableListOf<BaseElement>()
    val open = false

    init {
        x = initX
        y = initY
    }

    fun update() {
        height = if (open) getEH() + 15f else 15f
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

        nvg.rect(0f, 0f, width, 15f, Color.BLACK.withAlpha(150).rgb)
        nvg.text(subcategory.subName, 3f, 2.5f, 10f, Color.WHITE.rgb, nvg.montserrat)

        if (open) {
            elements.forEach {
                it.render(context, mouseX, mouseY, delta)
            }
        }

        nvg.pop()
    }
}