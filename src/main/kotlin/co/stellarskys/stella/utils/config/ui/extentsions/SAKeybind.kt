package co.stellarskys.stella.utils.config.ui.extentsions

import dev.deftu.omnicore.api.client.input.OmniKeys
import co.stellarskys.vexel.components.core.Rectangle
import co.stellarskys.vexel.components.core.Text
import co.stellarskys.vexel.components.base.enums.Pos
import co.stellarskys.vexel.components.base.enums.Size
import co.stellarskys.vexel.components.base.VexelElement
import org.lwjgl.glfw.GLFW

class SAKeybind(
    backgroundColor: Int = 0x80404040.toInt(),
    borderColor: Int = 0xFF606060.toInt(),
    borderRadius: Float = 4f,
    borderThickness: Float = 1f,
    padding: FloatArray = floatArrayOf(12f, 24f, 12f, 24f),
    hoverColor: Int? = 0x80505050.toInt(),
    pressedColor: Int? = 0x80303030.toInt(),
    widthType: Size = Size.Auto,
    heightType: Size = Size.Auto
) : VexelElement<SAKeybind>(widthType, heightType) {
    var selectedKeyId: Int? = null
        set(value) {
            field = value
            innerText.text = getKeyName(value ?: 0, selectedScanId ?: 0)
        }

    var selectedScanId: Int? = null
    var listen: Boolean = false

    val background = Rectangle(
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

    val innerText = Text(getKeyName(OmniKeys.KEY_A.code, 0), 0xFFFFFFFF.toInt(), 12f)
        .setPositioning(Pos.ParentCenter, Pos.ParentCenter)
        .childOf(background)

    init {
        setSizing(100f, Size.Pixels, 0f, Size.Auto)
        setPositioning(Pos.ParentPixels, Pos.ParentPixels)
        ignoreFocus()

        onClick { _ ->
            listenForKeybind()
            true
        }

        onCharType { event ->
            if (!listen) return@onCharType false

            if (event.keyCode == 256) {
                innerText.text = "None"
                selectedKeyId = null
                selectedScanId = null
            } else {
                innerText.text = getKeyName(event.keyCode, event.scanCode)
                selectedKeyId = event.keyCode
                selectedScanId = event.scanCode
            }

            onValueChange.forEach { it.invoke(event.keyCode) }
            listen = false
            true
        }
    }

    fun listenForKeybind() {
        innerText.text = "..."
        listen = true
    }

    override fun onRender(mouseX: Float, mouseY: Float) {
        background.isHovered = isHovered
        background.isPressed = isPressed
    }

    private fun getKeyName(keyCode: Int, scanCode: Int): String = when(keyCode) {
        0 -> "None"
        else -> getKeyStringName(keyCode, scanCode)
    }

    override fun getAutoWidth(): Float = background.getAutoWidth()
    override fun getAutoHeight(): Float = background.getAutoHeight()

    fun padding(top: Float, right: Float, bottom: Float, left: Float): SAKeybind = apply {
        background.padding(top, right, bottom, left)
    }

    fun padding(all: Float): SAKeybind = apply {
        background.padding(all)
    }

    fun backgroundColor(color: Int): SAKeybind = apply {
        background.backgroundColor(color)
    }

    fun borderColor(color: Int): SAKeybind = apply {
        background.borderColor(color)
    }

    fun borderRadius(radius: Float): SAKeybind = apply {
        background.borderRadius(radius)
    }

    fun borderThickness(thickness: Float): SAKeybind = apply {
        background.borderThickness(thickness)
    }

    fun hoverColor(color: Int): SAKeybind = apply {
        background.hoverColor(color)
    }

    fun pressedColor(color: Int): SAKeybind = apply {
        background.pressedColor(color)
    }

    private fun getKeyStringName(keyCode: Int, scanCode: Int): String = when (keyCode) {
        340 -> "LShift"
        344 -> "RShift"
        341 -> "LCtrl"
        345 -> "RCtrl"
        342 -> "LAlt"
        346 -> "RAlt"
        257 -> "Enter"
        256 -> "Escape"
        in 290..301 -> "F${keyCode - 289}" // F1–F12
        else -> {
            GLFW.glfwGetKeyName(keyCode, scanCode)?.let {
                if (it.length == 1) it.uppercase() else it
            } ?: "Key$keyCode"
        }
    }
}