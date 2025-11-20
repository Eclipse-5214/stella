package co.stellarskys.stella.hud

import net.minecraft.client.gui.GuiGraphics
import co.stellarskys.stella.hud.HUDManager.customRenderers
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.height
import co.stellarskys.stella.utils.render.Render2D.width
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.input.KnitMouse
import xyz.meowing.knit.api.screen.KnitScreen
import java.awt.Color

class HUDEditor : KnitScreen("HUD Editor") {
    private val borderHoverColor = Color(255, 255, 255).rgb
    private val borderNormalColor = Color(100, 100, 120).rgb

    private var dragging: HUDElement? = null
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onInitGui() {
        super.onInitGui()
        HUDManager.loadAllLayouts()
    }

    override fun onCloseGui() {
        super.onCloseGui()
        HUDManager.saveAllLayouts()
    }

    override fun onRender(context: GuiGraphics?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        val context = context ?: return
        context.fill(0, 0, width, width, 0x90000000.toInt())

        HUDManager.elements.values.forEach { element ->
            if (!element.isEnabled()) return@forEach

            context.pose().pushMatrix()
            context.pose().translate(element.x, element.y)
            context.pose().scale(element.scale, element.scale)

            val isHovered = element.isHovered(mouseX.toFloat(), mouseY.toFloat())

            val borderColor = if (isHovered) borderHoverColor else borderNormalColor

            val alpha = if (isHovered) 140 else 90
            val custom = customRenderers[element.id]

            if (custom != null) {
                drawHollowRect(context, 0, 0, element.width, element.height, borderColor)
                context.fill(0,0, element.width, element.height, Color(30, 35, 45, alpha).rgb)
                custom(context)
            } else {
                if (element.width == 0 && element.height == 0) {
                    element.width = element.text.width() + 2
                    element.height = element.text.height() + 2
                }

                drawHollowRect(context, -2, -3, element.width, element.height, borderColor)
                context.fill(-2,-3, element.width, element.height, Color(30, 35, 45, alpha).rgb)

                Render2D.drawString(context, element.text, 0, 0, shadow = false)
            }

            context.pose().popMatrix()
        }

        Render2D.drawString(context, "Drag elements. Press ESC to exit.", 10, 10)
    }

    override fun onMouseClick(mouseX: Int, mouseY: Int, button: Int) {
        val hovered = HUDManager.elements.values.firstOrNull { it.isHovered(mouseX.toFloat(), mouseY.toFloat()) }
        if (hovered != null) {
            dragging = hovered
            offsetX = mouseX.toFloat() - hovered.x
            offsetY = mouseY.toFloat() - hovered.y
        }
    }

    override fun onMouseMove(mouseX: Int, mouseY: Int) {
        dragging?.let {
            it.x = KnitMouse.Scaled.x.toFloat() - offsetX
            it.y = KnitMouse.Scaled.y.toFloat() - offsetY
        }
    }

    override fun onMouseRelease(mouseX: Int, mouseY: Int, button: Int) {
        dragging = null
    }

    override fun onMouseScroll(horizontal: Double, vertical: Double) {
        val hovered = HUDManager.elements.values.firstOrNull { it.isHovered(KnitMouse.Scaled.x.toFloat(), KnitMouse.Scaled.y.toFloat()) }

        if (hovered != null) {
            val scaleDelta = if (vertical > 0) 0.1f else -0.1f
            hovered.scale = (hovered.scale + scaleDelta).coerceIn(0.2f, 5.0f)
        }
    }

    override fun isPauseScreen(): Boolean = false

    private fun drawHollowRect(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y1 + 1, color)
        context.fill(x1, y2 - 1, x2, y2, color)
        context.fill(x1, y1, x1 + 1, y2, color)
        context.fill(x2 - 1, y1, x2, y2, color)
    }
}