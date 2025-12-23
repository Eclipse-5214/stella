package co.stellarskys.stella.utils.config.ui.extentsions

import co.stellarskys.stella.utils.config.ui.Palette
import co.stellarskys.stella.utils.config.ui.Palette.withAlpha
import co.stellarskys.vexel.animations.presets.fadeIn
import co.stellarskys.vexel.animations.presets.fadeOut
import co.stellarskys.vexel.animations.types.EasingType
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.SvgImage
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import co.stellarskys.vexel.elements.DropDownPanel
import java.awt.Color

class SADropdown(
    var options: List<String>,
    var selectedIndex: Int = 0,
    backgroundColor: Int = 0xFF282e3a.toInt(),
    iconColor: Int = 0xFF4c87f9.toInt(),
    borderColor: Int = 0xFF0194d8.toInt(),
    borderRadius: Float = 6f,
    borderThickness: Float = 2f,
    padding: FloatArray = floatArrayOf(6f, 6f, 6f, 6f),
    hoverColor: Int? = 0x80505050.toInt(),
    pressedColor: Int? = 0x80303030.toInt(),
    widthType: Size = Size.Pixels,
    heightType: Size = Size.Pixels
): VexelElement<SADropdown>(widthType, heightType) {
    var fontSize = 12f
    var selectedTextColor = 0xFFFFFFFF.toInt()
    var dropdownIconPath = "/assets/stella/logos/dropdown.svg"
    var isPickerOpen = false
    var isAnimating = false
    private var lastPosition = Pair(0f, 0f)

    val previewRect = Rectangle(
        backgroundColor,
        borderColor,
        borderRadius,
        borderThickness,
        padding,
        hoverColor,
        pressedColor,
        Size.Percent,
        Size.Percent
    )
        .setSizing(100f, Size.Percent, 100f, Size.Percent)
        .ignoreMouseEvents()
        .childOf(this)

    val selectedText = Text(options[selectedIndex], selectedTextColor, fontSize)
        .setPositioning(Pos.ParentPixels, Pos.ParentCenter)
        .childOf(previewRect)

    val dropdownArrow = SvgImage(svgPath = dropdownIconPath, color = Color(iconColor))
        .setSizing(20f, Size.Pixels, 20f, Size.Pixels)
        .setPositioning(5f, Pos.ParentPixels, 0f, Pos.ParentCenter)
        .alignRight()
        .childOf(previewRect)

    var pickerPanel: DropDownPanel? = null

    init {
        setSizing(180f, Size.Pixels, 0f, Size.Auto)
        setPositioning(Pos.ParentPixels, Pos.ParentPixels)

        onClick { _ ->
            if (!isAnimating) togglePicker()
            true
        }
    }

    fun togglePicker() {
        if (isPickerOpen) closePicker() else openPicker()
    }

    fun openPicker() {
        if (isPickerOpen || isAnimating) return
        isAnimating = true

        dropdownArrow.rotateTo(180f, 200)

        val currentX = previewRect.getScreenX()
        val currentY = previewRect.getScreenY() + previewRect.height + 4f
        lastPosition = Pair(currentX, currentY)

        pickerPanel = DropDownPanel(
            selectedIndex,
            options,
            Color.BLACK.rgb,
            Palette.Purple.withAlpha(100).rgb,
            fontSize = fontSize,
            sourceDropdown = previewRect
        )
            .setSizing(previewRect.width, Size.Pixels, 0f, Size.Auto)
            .setPositioning(currentX, Pos.ScreenPixels, currentY, Pos.ScreenPixels)
            .childOf(getRootElement())

        pickerPanel?.onValueChange { index ->
            selectedIndex = index as Int
            selectedText.text = options[selectedIndex]
            closePicker()
            onValueChange.forEach { it.invoke(index) }
        }

        pickerPanel?.backgroundPopup?.fadeIn(200, EasingType.EASE_OUT ) {
            isAnimating = false
        }

        isPickerOpen = true
    }

    override fun getAutoWidth(): Float {
        return previewRect.getAutoWidth()
    }

    override fun getAutoHeight(): Float {
        return previewRect.getAutoHeight()
    }

    fun closePicker() {
        if (!isPickerOpen || pickerPanel == null || isAnimating) return
        isAnimating = true

        dropdownArrow.rotateTo(0f, 200)

        pickerPanel?.backgroundPopup?.fadeOut(200, EasingType.EASE_IN) {
            getRootElement().children.remove(pickerPanel!!)
            pickerPanel!!.destroy()
            pickerPanel = null
            isAnimating = false
        }

        isPickerOpen = false
    }

    override fun handleMouseClick(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val handled = super.handleMouseClick(mouseX, mouseY, button)

        if (isPickerOpen && pickerPanel != null && !pickerPanel!!.isPointInside(mouseX, mouseY) && !isPointInside(mouseX, mouseY) && !isAnimating) {
            closePicker()
        }

        return handled
    }

    private fun updatePickerPosition() {
        val currentX = previewRect.getScreenX()
        val currentY = previewRect.getScreenY() + previewRect.height + 4f

        if (lastPosition.first != currentX || lastPosition.second != currentY) {
            lastPosition = Pair(currentX, currentY)
            pickerPanel?.setPositioning(currentX, Pos.ScreenPixels, currentY, Pos.ScreenPixels)
        }
    }

    override fun onRender(mouseX: Float, mouseY: Float) {
        previewRect.isHovered = isHovered
        previewRect.isPressed = isHovered

        if (isPickerOpen && pickerPanel != null) {
            if (!previewRect.isVisibleInScrollableParents()) closePicker() else updatePickerPosition()
        }
    }

    override fun destroy() {
        if (isPickerOpen) closePicker()
        super.destroy()
    }

    fun fontSize(size: Float): SADropdown = apply {
        fontSize = size
        selectedText.fontSize = size
    }


    fun selectedTextColor(color: Int) {
        selectedTextColor = color
    }

    fun setArrowIconPath(path: String) {
        dropdownIconPath = path
    }
}