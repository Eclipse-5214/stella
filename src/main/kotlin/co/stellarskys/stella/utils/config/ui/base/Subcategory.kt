package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config.core.ConfigSubcategory
import co.stellarskys.stella.utils.config.ui.ConfigUI
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.render.nvg.Image
import net.minecraft.client.gui.GuiGraphics
import java.awt.Color
import java.util.UUID

class Subcategory(initX: Float, initY: Float, val subcategory: ConfigSubcategory): ParentElement() {
    var open = false
    val value get() = subcategory.value as Boolean
    var buttonColor by Utils.animate<Color>(0.15)
    var textColor by Utils.animate<Color>(0.15)
    var dropdownRot by Utils.animate<Double>(0.15)
    val offsetDeleagte = Utils.animate<Float>(0.15, error = 0.1)
    var elementOffset by offsetDeleagte

    init {
        x = initX
        y = initY
        buttonColor = if (value) Palette.Purple else Palette.Mantle
        textColor = if (value) Palette.Mantle else Palette.Text
        dropdownRot = if (open) 0.0 else -90.0
    }

    override fun update() {
        height = if (open || !offsetDeleagte.done()) HEIGHT + elementOffset + getEH() else HEIGHT
    }

    override fun render(
        context: GuiGraphics,
        mouseX: Float,
        mouseY: Float,
        delta: Float
    ) {
        if (isAnimating) {
            update()
            updateElements(elementOffset + HEIGHT)
            if (offsetDeleagte.done()) {
                isAnimating = false
            }
        }

        nvg.push()
        nvg.translate(x, y)

        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.rgb)
        nvg.rect(2f, 2f, width - 4f, HEIGHT - 4, buttonColor.rgb, 5f)
        nvg.text(subcategory.subName, 6f, 8.5f, 8f, textColor.rgb, nvg.inter)

        if (!visibleElements.isEmpty()) {
            nvg.push()
            nvg.translate(width - 10f, 12.5f)
            nvg.rotate(Math.toRadians(dropdownRot).toFloat())
            nvg.image(ConfigUI.caretImage, -5f, -5f, 10f, 10f, textColor.rgb)
            nvg.pop()
        }

        if (open || !offsetDeleagte.done()) {
            nvg.pushScissor(0f, 0f + HEIGHT, width, getEH() + elementOffset)
            elements.forEach {
                it.render(context, mouseX, mouseY, delta)
            }
            nvg.popScissor()
        }

        nvg.pop()
    }

    companion object {
        const val HEIGHT = 25f
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (isAreaHovered(2f, 2f, width - 4f, HEIGHT - 4)) {
            if (button == 0 && !subcategory.configName.isEmpty()) {
                subcategory.value = !value
                if (value) {
                    buttonColor = Palette.Purple
                    textColor = Palette.Mantle
                } else {
                    buttonColor = Palette.Mantle
                    textColor = Palette.Text
                }
            } else {
                if(open) {
                    open = false
                    dropdownRot = -90.0
                    elementOffset = 0f
                    offsetDeleagte.snap()
                    elementOffset = -getEH()
                } else {
                    open = true
                    dropdownRot = 0.0
                    elementOffset = -getEH()
                    offsetDeleagte.snap()
                    elementOffset = 0f

                }

                isAnimating = true
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }
}