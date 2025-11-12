package co.stellarskys.stella.hud

import gg.essential.elementa.ElementaVersion
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.UIImage
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.CenterConstraint
import gg.essential.elementa.dsl.childOf
import gg.essential.elementa.dsl.pixels
import gg.essential.universal.UMatrixStack
import co.stellarskys.stella.hud.HUDManager.setPosition
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.gui.screen.Screen
import net.minecraft.text.Text
import org.lwjgl.glfw.GLFW
import java.awt.Color
import kotlin.math.roundToInt

class HUDEditor : Screen(Text.literal("HUD Editor")) {
    private val elements = mutableListOf<HUDElement>()
    private var dragging: HUDElement? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f
    private var selected: HUDElement? = null
    private var showGrid = true
    private var snapToGrid = true
    private var gridSize = 10
    private var previewMode = false
    private var showProperties = true
    private var showElements = true
    private var showToolbar = true
    private var showResetConfirm = false
    private val undoStack = mutableListOf<Map<String, HUDPosition>>()
    private val redoStack = mutableListOf<Map<String, HUDPosition>>()
    private var dirty = false
    private var hoveredToolbarIndex = -1
    private val toolbarIcons = listOf(
        "/assets/stella/logos/HUDEditor/grid.png",
        "/assets/stella/logos/HUDEditor/snap.png",
        "/assets/stella/logos/HUDEditor/preview.png",
        "/assets/stella/logos/HUDEditor/props.png",
        "/assets/stella/logos/HUDEditor/list.png",
        "/assets/stella/logos/HUDEditor/reset.png"
    )
    private val toolbarTooltips = listOf(
        "Toggle Grid",
        "Toggle Snap to Grid",
        "Toggle Preview Mode",
        "Toggle Properties Panel",
        "Toggle Element List",
        "Reset All Elements"
    )
    private val window = Window(ElementaVersion.V10)
    private val toolbarContainer = UIContainer().apply {
        setX(10.pixels())
        setY(5.pixels())
        setWidth(400.pixels())
        setHeight(20.pixels())
    } childOf window

    override fun init() {
        super.init()
        elements.clear()
        loadElements()
        saveState()
        dirty = false
        setupToolbarIcons()
    }

    override fun close() {
        super.close()
        if (dirty) {
            elements.forEach { element ->
                setPosition(element.name, element.targetX, element.targetY, element.scale, element.enabled)
            }
        }
    }

    private fun setupToolbarIcons() {
        toolbarContainer.clearChildren()

        toolbarIcons.forEachIndexed { index, iconPath ->
            val iconContainer = UIContainer().apply {
                setX((index * 28).pixels())
                setY(0.pixels())
                setWidth(24.pixels())
                setHeight(20.pixels())
            } childOf toolbarContainer

            UIImage.ofResource(iconPath).apply {
                setX(CenterConstraint())
                setY(CenterConstraint())
                setWidth(16.pixels())
                setHeight(16.pixels())
            } childOf iconContainer

            iconContainer.onMouseEnter {
                hoveredToolbarIndex = index
            }

            iconContainer.onMouseLeave {
                hoveredToolbarIndex = -1
            }

            iconContainer.onMouseClick { event ->
                when (index) {
                    0 -> showGrid = !showGrid
                    1 -> snapToGrid = !snapToGrid
                    2 -> previewMode = !previewMode
                    3 -> showProperties = !showProperties
                    4 -> showElements = !showElements
                    5 -> showResetConfirm = true
                }
            }
        }
    }

    private fun loadElements() {
        HUDManager.getElements().forEach { (name, text) ->
            val lines = text.split("\n")
            val width = lines.maxOfOrNull { textRenderer.getWidth(it) } ?: 0
            val height = lines.size * textRenderer.fontHeight + 10
            elements.add(HUDElement(name, HUDManager.getX(name), HUDManager.getY(name), width + 10, height, text, HUDManager.getScale(name), HUDManager.isEnabled(name)))
        }
    }

    override fun render(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {
        renderBackground(context, mouseX, mouseY, delta)

        if (showGrid && !previewMode) drawGrid(context)
        elements.forEach { it.render(context, mouseX.toFloat(), mouseY.toFloat(), delta, previewMode) }

        if (!previewMode) {
            if (showToolbar) {
                drawToolbar(context)
                window.draw(UMatrixStack())
            } else {
                drawToolbarToggleTooltip(context)
            }
            if (showElements) drawElementList(context, mouseX, mouseY)
            if (showProperties) selected?.let { drawProperties(context, it) }
            drawTooltips(context)
            if (hoveredToolbarIndex >= 0) drawToolbarTooltip(context, mouseX, mouseY, hoveredToolbarIndex)
        } else {
            drawPreviewHint(context)
        }

        if (showResetConfirm) {
            drawResetConfirmation(context, mouseX, mouseY)
        }

        super.render(context, mouseX, mouseY, delta)
    }

    private fun drawToolbarTooltip(context: DrawContext, mouseX: Int, mouseY: Int, index: Int) {
        val text = toolbarTooltips[index]
        val textWidth = textRenderer.getWidth(text)
        val textHeight = textRenderer.fontHeight
        val padding = 4
        val tooltipWidth = textWidth + padding * 2
        val tooltipHeight = textHeight + padding * 2

        var x = mouseX - tooltipWidth
        var y = mouseY - tooltipHeight

        x = x.coerceIn(0, width - tooltipWidth)
        y = y.coerceIn(0, height - tooltipHeight)

        context.fill(x, y, x + tooltipWidth, y + tooltipHeight, Color(30, 30, 40, 220).rgb)
        drawHollowRect(context, x, y, x + tooltipWidth, y + tooltipHeight, Color(100, 180, 255, 255).rgb)
        context.drawTextWithShadow(textRenderer, text, x + padding, y + padding, Color.WHITE.rgb)
    }

    private fun drawGrid(context: DrawContext) {
        val color = Color(60, 60, 80, 100).rgb

        for (x in 0 until width step gridSize) {
            context.drawVerticalLine(x, 0, height - 1, color)
        }

        for (y in 0 until height step gridSize) {
            context.drawHorizontalLine(0, width - 1, y, color)
        }
    }

    private fun drawPreviewHint(context: DrawContext) {
        val text = "Press P to exit preview mode"
        val textWidth = textRenderer.getWidth(text)
        val x = (width - textWidth) / 2
        val y = 10
        context.fill(x - 5, y - 3, x + textWidth + 5, y + 13, Color(0, 0, 0, 180).rgb)
        context.drawTextWithShadow(textRenderer, text, x, y, Color.WHITE.rgb)
    }

    private fun drawToolbarToggleTooltip(context: DrawContext) {
        val text = "Press T to toggle toolbar"
        val textWidth = textRenderer.getWidth(text)
        val x = 15
        val y = 10
        context.fill(x - 5, y - 3, x + textWidth + 5, y + 13, Color(0, 0, 0, 180).rgb)
        context.drawTextWithShadow(textRenderer, text, x, y, Color(100, 180, 255).rgb)
    }

    private fun drawToolbar(context: DrawContext) {
        val height = 30
        context.fill(0, 0, width, height, Color(20, 20, 30, 220).rgb)
        context.fill(0, height, width, height + 2, Color(70, 130, 180, 255).rgb)

        val toolbarStates = listOf(showGrid, snapToGrid, previewMode, showProperties, showElements, false)

        val title = "Stella - HUD Editor"
        val textWidth = textRenderer.getWidth(title)
        val titleX = width - textWidth - 15

        context.drawTextWithShadow(textRenderer, title, titleX, 10, Color(100, 180, 255).rgb)

        toolbarStates.forEachIndexed { index, isActive ->
            if (isActive) {
                val x = 10 + (index * 28)
                context.fill(x, height - 3, x + 24, height, Color(100, 180, 255).rgb)
            }
        }
    }

    private fun drawElementList(context: DrawContext, mouseX: Int, mouseY: Int) {
        val listWidth = 200
        val elementHeight = 16
        val headerHeight = 25
        val padding = 10
        val listHeight = minOf(elements.size * elementHeight + headerHeight + padding, height - 100)
        val listX = width - listWidth - 15
        val listY = if (showToolbar) 40 else 15

        context.fill(listX, listY, listX + listWidth, listY + listHeight, Color(20, 20, 30, 180).rgb)
        drawHollowRect(context, listX, listY, listX + listWidth, listY + listHeight, Color(70, 130, 180, 255).rgb)

        context.drawTextWithShadow(textRenderer, "HUD Elements", listX + 10, listY + 8, Color(180, 220, 255).rgb)

        val scrollOffset = if (elements.size * elementHeight > listHeight - headerHeight - padding) {
            maxOf(0, elements.size * elementHeight - (listHeight - headerHeight - padding))
        } else 0

        elements.forEachIndexed { index, element ->
            val elementY = listY + headerHeight + index * elementHeight - scrollOffset
            if (elementY < listY + headerHeight || elementY > listY + listHeight - elementHeight) return@forEachIndexed

            val isSelected = element == selected
            val isHovered = mouseX in listX..(listX + listWidth) && mouseY in elementY..(elementY + elementHeight)

            when {
                isSelected -> context.fill(listX + 5, elementY - 1, listX + listWidth - 5, elementY + elementHeight - 1, Color(40, 90, 140, 150).rgb)
                isHovered -> context.fill(listX + 5, elementY - 1, listX + listWidth - 5, elementY + elementHeight - 1, Color(50, 50, 70, 80).rgb)
            }

            val nameColor = if (element.enabled) Color(220, 240, 255).rgb else Color(150, 150, 170).rgb
            val displayName = element.name.take(17) + if (element.name.length > 17) "..." else ""
            context.drawText(textRenderer, displayName, listX + 10, elementY + 3, nameColor, false)

            val toggleText = if (element.enabled) "ON" else "OFF"
            val toggleColor = if (element.enabled) Color(100, 220, 100).rgb else Color(220, 100, 100).rgb
            context.drawText(textRenderer, toggleText, listX + listWidth - 30, elementY + 3, toggleColor, false)
        }
    }

    private fun drawProperties(context: DrawContext, element: HUDElement) {
        val width = 140
        val height = 75
        val x = 15
        val y = this.height - height - 15

        context.fill(x, y, x + width, y + height, Color(20, 20, 30, 180).rgb)
        drawHollowRect(context, x, y, x + width, y + height, Color(70, 130, 180, 255).rgb)

        context.drawTextWithShadow(textRenderer, "Properties", x + 10, y + 10, Color(100, 180, 255).rgb)
        context.drawTextWithShadow(textRenderer, "Position: ${element.targetX.toInt()}, ${element.targetY.toInt()}", x + 15, y + 25, Color.WHITE.rgb)
        context.drawTextWithShadow(textRenderer, "Scale: ${"%.1f".format(element.scale)}", x + 15, y + 40, Color.WHITE.rgb)
        context.drawTextWithShadow(textRenderer, if (element.enabled) "§aEnabled" else "§cDisabled", x + 15, y + 55, Color.WHITE.rgb)
    }

    private fun drawResetConfirmation(context: DrawContext, mouseX: Int, mouseY: Int) {
        val popupWidth = 280
        val popupHeight = 120
        val popupX = (width - popupWidth) / 2
        val popupY = (height - popupHeight) / 2

        //#if MC >= 1.21.7
        //$$ context.matrices.pushMatrix()
        //#else
        context.matrices.pushMatrix()
        //#endif
        //#if MC == 1.21.5
        //$$ context.matrices.translate(0f, 0f, 300f)
        //#endif
        context.fill(0, 0, width, height, Color(0, 0, 0, 120).rgb)
        context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, Color(25, 25, 35, 240).rgb)
        drawHollowRect(context, popupX, popupY, popupX + popupWidth, popupY + popupHeight, Color(70, 130, 180, 255).rgb)

        val titleText = "Reset All Elements"
        val titleX = popupX + (popupWidth - textRenderer.getWidth(titleText)) / 2
        context.drawTextWithShadow(textRenderer, titleText, titleX, popupY + 15, Color(220, 100, 100).rgb)

        val messageText = "This will reset all HUD elements to"
        val messageText2 = "default positions and enable them."
        val messageX = popupX + (popupWidth - textRenderer.getWidth(messageText)) / 2
        val messageX2 = popupX + (popupWidth - textRenderer.getWidth(messageText2)) / 2
        context.drawText(textRenderer, messageText, messageX, popupY + 40, Color(200, 200, 200).rgb, false)
        context.drawText(textRenderer, messageText2, messageX2, popupY + 55, Color(200, 200, 200).rgb, false)

        val buttonWidth = 80
        val buttonHeight = 20
        val buttonSpacing = 20
        val confirmX = popupX + (popupWidth / 2) - buttonWidth - (buttonSpacing / 2)
        val cancelX = popupX + (popupWidth / 2) + (buttonSpacing / 2)
        val buttonY = popupY + popupHeight - 35

        val confirmHovered = mouseX in confirmX..(confirmX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight)
        val cancelHovered = mouseX in cancelX..(cancelX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight)

        val confirmBg = if (confirmHovered) Color(200, 80, 80, 200).rgb else Color(170, 60, 60, 180).rgb
        val cancelBg = if (cancelHovered) Color(60, 120, 180, 200).rgb else Color(40, 100, 160, 180).rgb

        context.fill(confirmX, buttonY, confirmX + buttonWidth, buttonY + buttonHeight, confirmBg)
        context.fill(cancelX, buttonY, cancelX + buttonWidth, buttonY + buttonHeight, cancelBg)

        drawHollowRect(context, confirmX, buttonY, confirmX + buttonWidth, buttonY + buttonHeight, Color(255, 120, 120, 255).rgb)
        drawHollowRect(context, cancelX, buttonY, cancelX + buttonWidth, buttonY + buttonHeight, Color(120, 180, 255, 255).rgb)

        val confirmText = "Reset"
        val cancelText = "Cancel"
        val confirmTextX = confirmX + (buttonWidth - textRenderer.getWidth(confirmText)) / 2
        val cancelTextX = cancelX + (buttonWidth - textRenderer.getWidth(cancelText)) / 2
        val textY = buttonY + 6

        context.drawTextWithShadow(textRenderer, confirmText, confirmTextX, textY, Color.WHITE.rgb)
        context.drawTextWithShadow(textRenderer, cancelText, cancelTextX, textY, Color.WHITE.rgb)

        context.matrices.popMatrix()
    }

    private fun drawTooltips(context: DrawContext) {
        val tooltip = when {
            selected != null -> "Scroll to scale, Arrow keys to move"
            else -> null
        }

        tooltip?.let { text ->
            val x = (width - textRenderer.getWidth(text)) / 2
            val y = height - 30
            context.fill(x - 5, y - 3, x + textRenderer.getWidth(text) + 5, y + 13, Color(0, 0, 0, 180).rgb)
            context.drawTextWithShadow(textRenderer, text, x, y, Color(100, 180, 255).rgb)
        }
    }

    override fun mouseScrolled(mouseX: Double, mouseY: Double, horizontalAmount: Double, verticalAmount: Double): Boolean {
        if (selected != null) {
            saveState()
            val scaleDelta = if (verticalAmount > 0) 0.1f else -0.1f
            selected!!.scale = (selected!!.scale + scaleDelta).coerceIn(0.2f, 5f)
            dirty = true
            return true
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (showResetConfirm) {
            handleResetConfirmClick(mouseX.toInt(), mouseY.toInt())
            return true
        }

        if (showToolbar) {
            window.mouseClick(mouseX, mouseY, button)
        }

        if (button == 0) {
            if ((!showToolbar || !handleToolbarClick(mouseY.toInt())) && (!showElements || !handleElementListClick(mouseX.toInt(), mouseY.toInt())))
                handleElementDrag(mouseX.toInt(), mouseY.toInt())
        } else if (button == 1) {
            elements.reversed().find { it.isMouseOver(mouseX.toFloat(), mouseY.toFloat()) }?.let {
                selected = it
            }
        }
        return super.mouseClicked(mouseX, mouseY, button)
    }

    private fun handleResetConfirmClick(mouseX: Int, mouseY: Int) {
        val popupWidth = 280
        val popupHeight = 120
        val popupX = (width - popupWidth) / 2
        val popupY = (height - popupHeight) / 2

        val buttonWidth = 80
        val buttonHeight = 20
        val buttonSpacing = 20
        val confirmX = popupX + (popupWidth / 2) - buttonWidth - (buttonSpacing / 2)
        val cancelX = popupX + (popupWidth / 2) + (buttonSpacing / 2)
        val buttonY = popupY + popupHeight - 35

        when {
            mouseX in confirmX..(confirmX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight) -> {
                resetAll()
                showResetConfirm = false
            }
            mouseX in cancelX..(cancelX + buttonWidth) && mouseY in buttonY..(buttonY + buttonHeight) -> {
                showResetConfirm = false
            }
            mouseX !in popupX..(popupX + popupWidth) || mouseY !in popupY..(popupY + popupHeight) -> {
                showResetConfirm = false
            }
        }
    }

    private fun handleToolbarClick(mouseY: Int): Boolean {
        return mouseY <= 30
    }

    private fun handleElementListClick(mouseX: Int, mouseY: Int): Boolean {
        val listWidth = 200
        val listX = width - listWidth - 15
        val listY = if (showToolbar) 40 else 15
        val elementHeight = 16
        val headerHeight = 25

        if (mouseX !in listX..(listX + listWidth)) return false

        val clickedIndex = (mouseY - listY - headerHeight) / elementHeight
        if (clickedIndex !in 0 until elements.size) return false

        val element = elements[clickedIndex]
        if (mouseX >= listX + listWidth - 40) {
            element.enabled = !element.enabled
            dirty = true
        } else {
            selected = element
        }
        return true
    }

    private fun handleElementDrag(mouseX: Int, mouseY: Int) {
        if (previewMode) return

        elements.reversed().find { it.isMouseOver(mouseX.toFloat(), mouseY.toFloat()) }?.let { element ->
            dragging = element
            selected = element
            dragOffsetX = mouseX - element.getRenderX()
            dragOffsetY = mouseY - element.getRenderY()
            saveState()
        }
    }

    override fun mouseDragged(mouseX: Double, mouseY: Double, button: Int, deltaX: Double, deltaY: Double): Boolean {
        dragging?.let { element ->
            var newX = mouseX - dragOffsetX
            var newY = mouseY - dragOffsetY

            if (snapToGrid) {
                newX = (newX / gridSize).roundToInt() * gridSize.toDouble()
                newY = (newY / gridSize).roundToInt() * gridSize.toDouble()
            }

            newX = newX.coerceIn(-20.0, (width - element.width).toDouble() + 20.0)
            newY = newY.coerceIn(-20.0, (height - element.height).toDouble() + 20.0)

            element.setPosition(newX.toFloat(), newY.toFloat())
            dirty = true
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)
    }

    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        if (showToolbar) {
            window.mouseRelease()
        }

        dragging?.let { element ->
            dragging = null
            dirty = true
        }
        return super.mouseReleased(mouseX, mouseY, button)
    }

    override fun keyPressed(keyCode: Int, scanCode: Int, modifiers: Int): Boolean {
        if (showResetConfirm) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_N) {
                showResetConfirm = false
            } else if (keyCode == GLFW.GLFW_KEY_Y || keyCode == GLFW.GLFW_KEY_ENTER) {
                resetAll()
                showResetConfirm = false
            }
            return true
        }

        when (keyCode) {
            GLFW.GLFW_KEY_ESCAPE -> {
                if (previewMode) previewMode = false
                else close()
                return true
            }
            GLFW.GLFW_KEY_G -> showGrid = !showGrid
            GLFW.GLFW_KEY_P -> previewMode = !previewMode
            GLFW.GLFW_KEY_R -> showResetConfirm = true
            GLFW.GLFW_KEY_T -> showToolbar = !showToolbar
            GLFW.GLFW_KEY_Z -> if (hasControlDown()) undo()
            GLFW.GLFW_KEY_Y -> if (hasControlDown()) redo()
            GLFW.GLFW_KEY_DELETE -> selected?.let { delete(it) }
            GLFW.GLFW_KEY_UP -> selected?.let { move(it, 0, -1) }
            GLFW.GLFW_KEY_DOWN -> selected?.let { move(it, 0, 1) }
            GLFW.GLFW_KEY_LEFT -> selected?.let { move(it, -1, 0) }
            GLFW.GLFW_KEY_RIGHT -> selected?.let { move(it, 1, 0) }
            GLFW.GLFW_KEY_EQUAL, GLFW.GLFW_KEY_KP_ADD -> selected?.let { scale(it, 0.1f) }
            GLFW.GLFW_KEY_MINUS, GLFW.GLFW_KEY_KP_SUBTRACT -> selected?.let { scale(it, -0.1f) }
        }
        return super.keyPressed(keyCode, scanCode, modifiers)
    }

    private fun scale(element: HUDElement, delta: Float) {
        saveState()
        element.scale = (element.scale + delta).coerceIn(0.2f, 5f)
        dirty = true
    }

    private fun move(element: HUDElement, deltaX: Int, deltaY: Int) {
        saveState()
        val moveAmount = if (hasShiftDown()) 10 else 1
        val newX = element.targetX + deltaX * moveAmount
        val newY = element.targetY + deltaY * moveAmount

        val clampedX = newX.coerceIn(-20f, (width - element.width).toFloat() + 20f)
        val clampedY = newY.coerceIn(-20f, (height - element.height).toFloat() + 20f)

        element.setPosition(clampedX, clampedY)
        dirty = true
    }

    private fun delete(element: HUDElement) {
        saveState()
        element.enabled = false
        selected = null
        dirty = true
    }

    private fun resetAll() {
        saveState()
        elements.forEach { element ->
            element.setPosition(50f, 50f)
            element.enabled = true
            element.scale = 1f
        }
        dirty = true
    }

    private fun saveState() {
        val state = elements.associate { it.name to HUDPosition(it.targetX, it.targetY, it.scale, it.enabled) }
        undoStack.add(state)
        if (undoStack.size > 20) undoStack.removeFirst()
        redoStack.clear()
    }

    private fun undo() {
        if (undoStack.size > 1) {
            redoStack.add(undoStack.removeLast())
            applyState(undoStack.last())
            dirty = true
        }
    }

    private fun redo() {
        if (redoStack.isNotEmpty()) {
            val state = redoStack.removeLast()
            undoStack.add(state)
            applyState(state)
            dirty = true
        }
    }

    private fun applyState(state: Map<String, HUDPosition>) {
        elements.forEach { element ->
            state[element.name]?.let { pos ->
                element.setPosition(pos.x, pos.y)
                element.scale = pos.scale
                element.enabled = pos.enabled
            }
        }
    }

    private fun drawHollowRect(context: DrawContext, x1: Int, y1: Int, x2: Int, y2: Int, color: Int) {
        context.fill(x1, y1, x2, y1 + 1, color)
        context.fill(x1, y2 - 1, x2, y2, color)
        context.fill(x1, y1, x1 + 1, y2, color)
        context.fill(x2 - 1, y1, x2, y2, color)
    }

    override fun shouldPause() = false
    override fun renderBackground(context: DrawContext, mouseX: Int, mouseY: Int, delta: Float) {}
}