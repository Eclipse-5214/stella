package co.stellarskys.stella.hud

import net.minecraft.client.gui.GuiGraphics
import co.stellarskys.stella.hud.HUDManager.customRenderers
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.height
import co.stellarskys.stella.utils.render.Render2D.width
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniMouse
import dev.deftu.omnicore.api.client.input.OmniMouseButton
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.screen.OmniScreen
import dev.deftu.textile.Text
import java.awt.Color

class HUDEditor : OmniScreen(Text.literal("HUD Editor")) {
    private val borderHoverColor = Color(255, 255, 255).rgb
    private val borderNormalColor = Color(100, 100, 120).rgb

    private var dragging: HUDElement? = null
    private var offsetX = 0f
    private var offsetY = 0f

    override fun onInitialize(width: Int, height: Int) {
        HUDManager.loadAllLayouts()
        super.onInitialize(width, height)
    }

    override fun onScreenClose() {
        super.onScreenClose()
        HUDManager.saveAllLayouts()
    }

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        val context = ctx.graphics ?: return
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

    override fun onMouseClick(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        val hovered = HUDManager.elements.filter { it.value.isEnabled() }.values.firstOrNull { it.isHovered(x.toFloat(), y.toFloat()) }

        if (hovered != null) {
            dragging = hovered
            offsetX = x.toFloat() - hovered.x
            offsetY = y.toFloat() - hovered.y
        }

        return super.onMouseClick(button, x, y, modifiers)
    }

    override fun onMouseDrag(
        button: OmniMouseButton,
        x: Double,
        y: Double,
        deltaX: Double,
        deltaY: Double,
        clickTime: Long,
        modifiers: KeyboardModifiers
    ): Boolean {
        dragging?.let {
            it.x = x.toFloat() - offsetX
            it.y = y.toFloat() - offsetY
        }

        return super.onMouseDrag(button, x, y, deltaX, deltaY, clickTime, modifiers)
    }

    override fun onMouseRelease(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        dragging = null
        return super.onMouseRelease(button, x, y, modifiers)
    }

    override fun onMouseScroll(x: Double, y: Double, amount: Double, horizontalAmount: Double): Boolean {
        val hovered = HUDManager.elements.filter { it.value.isEnabled() }.values.firstOrNull { it.isHovered(x.toFloat(), y.toFloat()) }

        if (hovered != null) {
            val scaleDelta = if (amount > 0) 0.1f else -0.1f
            hovered.scale = (hovered.scale + scaleDelta).coerceIn(0.2f, 5.0f)
        }

        return super.onMouseScroll(x, y, amount, horizontalAmount)
    }

    override val isPausingScreen: Boolean = false

    private fun drawHollowRect(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y1 + 1, color)
        context.fill(x1, y2 - 1, x2, y2, color)
        context.fill(x1, y1, x1 + 1, y2, color)
        context.fill(x2 - 1, y1, x2, y2, color)
    }
}