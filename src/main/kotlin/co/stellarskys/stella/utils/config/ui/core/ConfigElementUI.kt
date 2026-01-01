package co.stellarskys.stella.utils.config.ui.core

import co.stellarskys.stella.utils.render.nvg.NVGRenderer
import dev.deftu.omnicore.api.client.input.OmniMouse
import dev.deftu.omnicore.api.client.render.OmniResolution

open class ConfigElementUI {
    var x = 0f
    var y = 0f
    var width = 0f
    var height = 0f

    val nvg get() = NVGRenderer

    val sw get() = OmniResolution.windowWidth.toFloat()
    val sh get() = OmniResolution.windowHeight.toFloat()
    val sf get() = OmniResolution.scaleFactor

    val scx get() = sw /2 - getAWidth() / 2
    val scy get() = sh /2 - getAHeight() / 2

    val mouseX get() = OmniMouse.rawX.toFloat()
    val mouseY get() = OmniMouse.rawY.toFloat()

    open var hidden: Boolean = false

    val isHoverd get() = contains(mouseX, mouseY)

    open fun getAWidth(): Float = width
    open fun getAHeight(): Float = height

    open fun render() {}

    fun contains(px: Float, py: Float): Boolean = px in x..(x + width) && py in y..(y + height)
}