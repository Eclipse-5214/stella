package co.stellarskys.stella.utils.config.ui.elements

import co.stellarskys.stella.utils.Utils
import co.stellarskys.stella.utils.animation.AnimType
import co.stellarskys.stella.utils.config.core.Dropdown
import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.stella.utils.config.ui.base.BaseElement
import net.minecraft.client.gui.GuiGraphics
import java.util.*

class DropdownUI(initX: Float, initY: Float, val dropdown: Dropdown) : BaseElement() {
    private var expansionAnim = Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var offsetAnim = Utils.animate<Float>(0.15)
    private var expansion by expansionAnim
    private var offset by offsetAnim
    private var caretRot by Utils.animate<Double>(0.15)
    private var hoveredIndex = -1
    private val dropdownPath = "/assets/stella/logos/dropdown.svg"
    private var caretImage = nvg.createImage(dropdownPath, 10, 10, Palette.Text, UUID.randomUUID().toString())

    init {
        x = initX
        y = initY
        width = 120f
        expansion = 0f
        caretRot = -90.0
        offset = if (visible) 0f else HEIGHT
        height = HEIGHT - offset
    }

    override fun setVisibility(value: Boolean) {
        super.setVisibility(value)
        offset = if (value) 0f else (HEIGHT + (dropdown.options.size * OPTION_HEIGHT) * expansion)
        isAnimating = true
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        if (!visible && !isAnimating) return
        val contentHeight = dropdown.options.size * OPTION_HEIGHT
        if (isAnimating) {
            reload()
            height = (HEIGHT + (contentHeight * expansion) - offset).coerceAtLeast(0f)
            if (expansionAnim.done() && offsetAnim.done()) isAnimating = false
        }

        nvg.push()
        nvg.translate(x, y)
        nvg.pushScissor(0f, 0f, width, height)
        nvg.rect(0f, 0f, width, HEIGHT, Palette.Crust.withAlpha(150).rgb)
        nvg.text(dropdown.name, 6f, 8.5f, 8f, Palette.Text.rgb, nvg.inter)

        nvg.rect(BOX_X, BOX_Y, BOX_W, BOX_H, Palette.Base.rgb, 3f)
        nvg.hollowRect(BOX_X, BOX_Y, BOX_W, BOX_H, 1f, Palette.Purple.withAlpha(100).rgb, 3f)
        nvg.pushScissor(BOX_X + 2f, BOX_Y, BOX_W - 14f, BOX_H)
        nvg.text(dropdown.options.getOrNull(dropdown.value as Int) ?: "None", BOX_X + 4f, BOX_Y + 3.5f, 7f, Palette.Text.rgb, nvg.inter)
        nvg.popScissor()

        nvg.push()
        nvg.translate(BOX_X + BOX_W - 7f, BOX_Y + (BOX_H / 2f))
        nvg.rotate(Math.toRadians(caretRot).toFloat())
        nvg.image(caretImage, -3.5f, -3.5f, 7f, 7f)
        nvg.pop()

        if (expansion > 0.01f) {
            nvg.pushScissor(0f, HEIGHT, width, height - HEIGHT)
            nvg.rect(0f, HEIGHT, width, contentHeight, Palette.Crust.withAlpha(100).rgb)
            dropdown.options.forEachIndexed { i, opt ->
                val optY = HEIGHT + (i * OPTION_HEIGHT)
                if (hoveredIndex == i) nvg.rect(4f, optY + 2f, width - 8f, OPTION_HEIGHT - 4f, Palette.Purple.withAlpha(100).rgb, 4f)
                if (i == dropdown.value as Int) nvg.rect(2f, optY + 4f, 1.5f, OPTION_HEIGHT - 8f, Palette.Purple.rgb, 1f)
                nvg.text(opt, 12f, optY + 6f, 7.5f, Palette.Text.rgb, nvg.inter)
            }
            nvg.popScissor()
        }

        nvg.popScissor()
        nvg.pop()

        hoveredIndex = if (expansion > 0.5f && isAreaHovered(0f, HEIGHT, width, contentHeight, mouseX, mouseY)) {
            ((mouseY - (absoluteY + HEIGHT)) / OPTION_HEIGHT).toInt().coerceIn(0, dropdown.options.size - 1)
        } else -1
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!visible || offset > 1f) return false
        if (isAreaHovered(BOX_X, BOX_Y, BOX_W, BOX_H, mouseX, mouseY)) {
            val opening = expansion < 0.5f
            expansion = if (opening) 1f else 0f
            caretRot = if (opening) 0.0 else -90.0
            isAnimating = true
            return true
        }

        if (expansion > 0.8f && hoveredIndex != -1) {
            dropdown.value = hoveredIndex
            expansion = 0f
            caretRot = -90.0
            isAnimating = true
            return true
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    fun reload() = nvg.deleteImage(caretImage).also { caretImage = nvg.createImage(dropdownPath, 10, 10, Palette.Text, UUID.randomUUID().toString()) }

    companion object {
        const val HEIGHT = 25f
        const val OPTION_HEIGHT = 18f
        const val BOX_W = 42f
        const val BOX_H = 14f
        const val BOX_X = 120f - BOX_W - 4f
        const val BOX_Y = (HEIGHT - BOX_H) / 2f
    }
}