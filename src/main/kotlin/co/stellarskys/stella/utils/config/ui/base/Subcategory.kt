package co.stellarskys.stella.utils.config.ui.base

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.config.core.ConfigSubcategory
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.render.nvg.Image
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup
import java.awt.Color
import java.util.UUID

class Subcategory(initX: Float, initY: Float, val subcategory: ConfigSubcategory): BaseElement() {
    val elements = mutableListOf<BaseElement>()
    val open = false
    val value get() = subcategory.value as Boolean
    var buttonColor by Utils.lerped<Color>(0.15)
    var textColor by Utils.lerped<Color>(0.15)
    var dropdownRot by Utils.lerped<Double>(0.15)
    private var dropdownPath = "/assets/stella/logos/dropdown.svg"
    private var image = nvg.createImage(dropdownPath,  8, 8, textColor, UUID.randomUUID().toString())

    init {
        x = initX
        y = initY
        buttonColor = if (value) Palette.Purple else Palette.Mantle
        textColor = if (value) Palette.Mantle else Palette.Text
        dropdownRot = if (open) 0.0 else -90.0
        reload()
    }

    fun update() {
        height = if (open) getEH() + HEIGHT else HEIGHT
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

        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.rgb)
        nvg.rect(2f, 2f, width - 4f, HEIGHT - 4, buttonColor.rgb, 5f)
        nvg.text(subcategory.subName, 6f, 8.5f, 8f, textColor.rgb, nvg.inter)
        nvg.push()
        nvg.translate(width - 10f - 4f, 12.5f - 4f)
        nvg.rotate(Math.toRadians(dropdownRot).toFloat())
        nvg.image(image, 0f, 0f, 8f, 8f)
        nvg.pop()

        if (open) {
            elements.forEach {
                it.render(context, mouseX, mouseY, delta)
            }
        }

        nvg.pop()
    }

    companion object {
        const val HEIGHT = 25f
    }

    fun reload(): Image {
        nvg.deleteImage(image)
        image = nvg.createImage(dropdownPath,  8, 8, textColor, UUID.randomUUID().toString())
        return image
    }
}