package co.stellarskys.stella.api.zenith

import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.input.CharacterEvent
import net.minecraft.client.input.KeyEvent
import net.minecraft.client.input.MouseButtonEvent
import net.minecraft.network.chat.Component

/**
 * Screen abstraction
 */
abstract class Aperture(title: String = "") : Screen(Component.literal(title)) {
    open val isPausingScreen: Boolean get() = false
    override fun isPauseScreen(): Boolean = isPausingScreen

    open fun onInitialize(width: Int, height: Int) {}
    open fun onResize(width: Int, height: Int) {}
    open fun onScreenTick() = super.tick()
    open fun onScreenClose() = super.onClose()
    open fun onRender(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) = super.extractRenderState(context, mouseX, mouseY, tickDelta)
    open fun onBackgroundRender(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {}

    open fun onMouseClick(button: Int, x: Double, y: Double, modifiers: Int): Boolean = false
    open fun onMouseRelease(button: Int, x: Double, y: Double, modifiers: Int): Boolean = false
    open fun onMouseDrag(button: Int, x: Double, y: Double, deltaX: Double, deltaY: Double, clickTime: Long, modifiers: Int): Boolean = false
    open fun onMouseScroll(x: Double, y: Double, amount: Double, horizontalAmount: Double): Boolean = false
    open fun onKeyPress(key: Int, scanCode: Int, modifiers: Int): Boolean = false
    open fun onCharTyped(char: Char): Boolean = false

    final override fun init() {
        super.init()
        onInitialize(width, height)
    }

    final override fun resize(width: Int, height: Int) {
        onResize(width, height)
        super.resize(width, height)
    }

    final override fun tick() = onScreenTick()
    final override fun onClose() = onScreenClose()
    final override fun extractRenderState(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) = onRender(graphics, mouseX, mouseY, a)
    final override fun extractBackground(graphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, a: Float) = onBackgroundRender(graphics, mouseX, mouseY, a)
    final override fun mouseClicked(event: MouseButtonEvent, isDoubleClick: Boolean): Boolean = onMouseClick(event.button(), event.x(), event.y(), event.modifiers()) || super.mouseClicked(event, isDoubleClick)
    final override fun mouseReleased(event: MouseButtonEvent): Boolean = onMouseRelease(event.button(), event.x(), event.y(), event.modifiers()) || super.mouseReleased(event)
    final override fun mouseDragged(event: MouseButtonEvent, deltaX: Double, deltaY: Double): Boolean = onMouseDrag(event.button(), event.x(), event.y(), deltaX, deltaY, 0L, event.modifiers()) || super.mouseDragged(event, deltaX, deltaY)
    final override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontal: Double, vertical: Double): Boolean = onMouseScroll(mouseX, mouseY, vertical, horizontal) || super.mouseScrolled(mouseX, mouseY, horizontal, vertical)
    final override fun keyPressed(event: KeyEvent): Boolean = onKeyPress(event.key(), event.scancode(), event.modifiers()) || super.keyPressed(event)
    final override fun charTyped(event: CharacterEvent): Boolean = onCharTyped(event.codepoint.toChar()) || super.charTyped(event)
}