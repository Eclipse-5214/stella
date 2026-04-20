package co.stellarskys.stella.hud

import co.stellarskys.stella.utils.config

class HUDElement(
    val id: String,
    var x: Float,
    var y: Float,
    var width: Int,
    var height: Int,
    var scale: Float = 1f,
    var text: String = "",
    var configKey: String? = null
) {
    fun isHovered(mouseX: Float, mouseY: Float): Boolean {
        val scaledWidth = width * scale
        val scaledHeight = height * scale
        return mouseX in x..(x + scaledWidth) && mouseY in y..(y + scaledHeight)
    }

    fun isEnabled(): Boolean {
        return configKey?.let {
            config[it] as? Boolean ?: false
        } ?: true
    }
}