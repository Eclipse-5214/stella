package co.stellarskys.stella.hud

import net.minecraft.client.gui.GuiGraphics
import co.stellarskys.stella.hud.HUDManager.customRenderers
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.height
import co.stellarskys.stella.utils.render.Render2D.width
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniKey
import dev.deftu.omnicore.api.client.input.OmniKeys
import dev.deftu.omnicore.api.client.input.OmniMouseButton
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.screen.KeyPressEvent
import dev.deftu.omnicore.api.client.screen.OmniScreen
import dev.deftu.textile.Text
import java.awt.Color

class HUDEditor : OmniScreen(Text.literal("HUD Editor")) {
    private val borderHoverColor = Color(255, 255, 255).rgb
    private val borderNormalColor = Color(100, 100, 120).rgb

    private var dragging: HUDElement? = null
    private var offsetX = 0f
    private var offsetY = 0f

    private val SNAP_DISTANCE = 2f
    private var snappingEnabled = false

    private var snapLines = mutableListOf<SnapLine>()

    private data class SnapLine(
        val x1: Float, val y1: Float,
        val x2: Float, val y2: Float,
        val color: Int = 0x80FFFFFF.toInt() // translucent white
    )

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
        context.fill(0, 0, width, height, 0x90000000.toInt())

        snapLines.forEach { line ->
            val isVert = line.x1 == line.x2
            context.fill(line.x1.toInt(), line.y1.toInt(), (if (isVert) line.x1 + 1 else line.x2).toInt(), (if (isVert) line.y2 else line.y1 + 1).toInt(), line.color)
        }

        if (dragging == null) snapLines.clear()

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
                    element.width = element.text.width() + 4
                    element.height = element.text.height() + 6
                }

                drawHollowRect(context, 0,  0, element.width, element.height, borderColor)
                context.fill(0,0, element.width, element.height, Color(30, 35, 45, alpha).rgb)

                Render2D.drawString(context, element.text, 2, 3, shadow = false)
            }

            context.pose().popMatrix()
        }

        Render2D.drawString(context, "Drag elements. Press ESC to exit.", 10, 10)
        Render2D.drawString(context, "Press S to enable Snapping.", 10, 20)
    }

    override fun onMouseDrag(button: OmniMouseButton, x: Double, y: Double, deltaX: Double, deltaY: Double, clickTime: Long, modifiers: KeyboardModifiers): Boolean {
        dragging?.let { element ->
            snapLines.clear()

            var newX = x.toFloat() - offsetX
            var newY = y.toFloat() - offsetY

            if (snappingEnabled) {
                val w = element.width * element.scale
                val h = element.height * element.scale

                val targets = mutableListOf(
                    Triple(0f, 0f, true), Triple(width - w, width.toFloat(), true), Triple(width / 2f - w / 2f, width / 2f, true),
                    Triple(0f, 0f, false), Triple(height - h, height.toFloat(), false), Triple(height / 2f - h / 2f, height / 2f, false)
                )

                HUDManager.elements.values.filter { it !== element && it.isEnabled() }.forEach { other ->
                    val ow = other.width * other.scale; val oh = other.height * other.scale
                    targets.add(Triple(other.x + ow, other.x + ow, true)) // Snap Left to Other's Right
                    targets.add(Triple(other.x - w, other.x, true))       // Snap Right to Other's Left
                    targets.add(Triple(other.x + ow/2f - w/2f, other.x + ow/2f, true)) // Centers
                    // Y-Axis
                    targets.add(Triple(other.y + oh, other.y + oh, false))
                    targets.add(Triple(other.y - h, other.y, false))
                    targets.add(Triple(other.y + oh/2f - h/2f, other.y + oh/2f, false))
                }

                targets.forEach { (target, guide, isX) ->
                    if (isX) newX = snap(newX, target, true, guide)
                    else newY = snap(newY, target, false, guide)
                }
            }

            element.x = newX
            element.y = newY
        }
        return super.onMouseDrag(button, x, y, deltaX, deltaY, clickTime, modifiers)
    }

    override fun onMouseRelease(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        dragging = null
        snapLines.clear()
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

    override fun onMouseClick(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        dragging = HUDManager.elements.values.firstOrNull { it.isEnabled() && it.isHovered(x.toFloat(), y.toFloat()) }?.also {
            offsetX = x.toFloat() - it.x; offsetY = y.toFloat() - it.y
        }
        return super.onMouseClick(button, x, y, modifiers)
    }

    override fun onKeyPress(key: OmniKey, scanCode: Int, typedChar: Char, modifiers: KeyboardModifiers, event: KeyPressEvent): Boolean {
        if (key == OmniKeys.KEY_S) snappingEnabled = !snappingEnabled.also { snapLines.clear() }
        return super.onKeyPress(key, scanCode, typedChar, modifiers, event)
    }
    override val isPausingScreen: Boolean = false

    private fun drawHollowRect(context: GuiGraphics, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y1 + 1, color)
        context.fill(x1, y2 - 1, x2, y2, color)
        context.fill(x1, y1, x1 + 1, y2, color)
        context.fill(x2 - 1, y1, x2, y2, color)
    }

    private fun snap(value: Float, target: Float, isX: Boolean, guidePos: Float): Float {
        if (kotlin.math.abs(value - target) <= SNAP_DISTANCE) {
            if (isX)  addSnapLine(guidePos, 0f, guidePos, height.toFloat())
            else addSnapLine(0f, guidePos, width.toFloat(), guidePos)
            return target
        }
        return value
    }

    private fun addSnapLine(x1: Float, y1: Float, x2: Float, y2: Float) {
        snapLines += SnapLine(x1, y1, x2, y2)
    }
}