package co.stellarskys.stella.features.secrets.utils.routes.editor

import co.stellarskys.stella.api.config.ui.ConfigUI
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.config.ui.base.ParentElement
import co.stellarskys.stella.api.config.ui.base.TextBox
import co.stellarskys.stella.api.config.ui.base.addTo
import co.stellarskys.stella.api.nvg.Gradient
import co.stellarskys.stella.api.nvg.NVGRenderer
import co.stellarskys.stella.api.zenith.Aperture
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.features.secrets.utils.routes.RouteRecorder
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.core.BlockPos

class WaypointEditor(val pos: BlockPos) : Aperture("Add Custom Waypoint") {
    private val nvg get() = NVGRenderer
    private val mouse = Zenith.Mouse
    private val mx get() = mouse.rawX.toFloat() / ConfigUI.Companion.UI_SCALE
    private val my get() = mouse.rawY.toFloat() / ConfigUI.Companion.UI_SCALE

    private val dialogPanel = object : ParentElement() {
        override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
            width = DIALOG_W; height = DIALOG_H
            x = (rez.windowWidth / ConfigUI.Companion.UI_SCALE) / 2f - DIALOG_W / 2f
            y = (rez.windowHeight / ConfigUI.Companion.UI_SCALE) / 2f - DIALOG_H / 2f

            nvg.push()
            nvg.translate(x, y)

            nvg.rect(0f, 0f, DIALOG_W, DIALOG_H, Palette.Crust.rgb, 12f)
            nvg.hollowGradientRect(0f, 0f, DIALOG_W, DIALOG_H, 2f, Palette.Purple.rgb, Palette.Mauve.rgb, Gradient.TopLeftToBottomRight, 12f)

            nvg.text("Add Custom Waypoint", PADDING, TITLE_Y, 18f, Palette.Text.rgb, nvg.inter)
            nvg.text("Position: (${pos.x}, ${pos.y}, ${pos.z})", PADDING, POS_Y, 13f, Palette.Subtext1.rgb, nvg.inter)
            nvg.rect(PADDING, DIV1_Y, DIALOG_W - PADDING * 2f, 1f, Palette.Surface0.rgb)

            nvg.text("Name", PADDING, NAME_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)
            nvg.rect(PADDING, DIV2_Y, DIALOG_W - PADDING * 2f, 1f, Palette.Surface0.rgb)

            nvg.text("Color", PADDING, COLOR_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)

            nvg.rect(PADDING, DEPTH_SECTION_Y, DIALOG_W - PADDING * 2f, 1f, Palette.Surface0.rgb)
            nvg.text("Depth Check", PADDING, DEPTH_SECTION_Y + 12f, 13f, Palette.Subtext0.rgb, nvg.inter)

            elements.forEach { it.render(context, mouseX, mouseY, delta) }

            nvg.pop()
        }
    }

    private var waypointName = ""
    private val nameBox: TextBox = TextBox(
        PADDING, NAME_BOX_Y, DIALOG_W - PADDING * 2f, NAME_BOX_H,
        initialText = "",
        color = Palette.Base.rgb,
        borderColor = Palette.Purple.withAlpha(50).rgb,
        focusColor = Palette.Purple.rgb,
        maxLength = 32,
        onType = { waypointName = it }
    ).addTo(dialogPanel)

    private val colorPicker = ColorPicker(DIALOG_W - PADDING * 2f)
        .apply { x = PADDING; y = COLOR_PICKER_Y }
        .addTo(dialogPanel)

    private val depthToggle = Toggle(true)
        .apply { width = TOGGLE_W; height = TOGGLE_H; x = DIALOG_W - PADDING - TOGGLE_W; y = TOGGLE_Y }
        .addTo(dialogPanel)

    private val cancelBtn = Button("Cancel", Palette.Surface1) { onClose() }
        .apply { width = BTN_W; height = BTN_H; x = PADDING; y = BTN_Y }
        .addTo(dialogPanel)

    private val confirmBtn = Button("Add Waypoint", Palette.Purple) { confirm() }
        .apply { width = BTN_W; height = BTN_H; x = DIALOG_W - PADDING - BTN_W; y = BTN_Y }
        .addTo(dialogPanel)

    override fun onRender(context: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {
        context.fill(0, 0, width, height, 0x90000000.toInt())
        context.drawNVG(false) {
            nvg.push()
            nvg.scale(ConfigUI.Companion.UI_SCALE, ConfigUI.Companion.UI_SCALE)
            dialogPanel.render(context, mx, my, tickDelta)
            nvg.pop()
        }
    }

    override fun onMouseClick(button: Int, x: Double, y: Double, modifiers: Int) =
        dialogPanel.mouseClicked(mx, my, button) || super.onMouseClick(button, x, y, modifiers)

    override fun onMouseRelease(button: Int, x: Double, y: Double, modifiers: Int): Boolean {
        dialogPanel.mouseReleased(mx, my, button)
        return super.onMouseRelease(button, x, y, modifiers)
    }

    override fun onKeyPress(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (key == 256) { onClose(); return true }
        return dialogPanel.keyPressed(key, modifiers) || super.onKeyPress(key, scanCode, modifiers)
    }

    override fun onCharTyped(char: Char) =
        dialogPanel.charTyped(char) || super.onCharTyped(char)

    private fun confirm() {
        RouteRecorder.addWaypoint(pos, waypointName, colorPicker.currentColor, depthToggle.value)
        onClose()
    }

    override fun onBackgroundRender(context: GuiGraphics, mouseX: Int, mouseY: Int, tickDelta: Float) {}

    companion object {
        const val DIALOG_W = 380f
        const val PADDING = 20f

        const val TITLE_Y = 20f
        const val POS_Y = 44f
        const val DIV1_Y = 65f
        const val NAME_LABEL_Y = 75f
        const val NAME_BOX_Y = 91f
        const val NAME_BOX_H = 28f
        const val DIV2_Y = 127f
        const val COLOR_LABEL_Y = 137f
        const val COLOR_PICKER_Y = 155f

        const val DEPTH_SECTION_Y = COLOR_PICKER_Y + ColorPicker.PICKER_HEIGHT + 8f
        const val TOGGLE_Y = DEPTH_SECTION_Y + 8f
        const val BTN_Y = DEPTH_SECTION_Y + 46f

        const val TOGGLE_W = 42f
        const val TOGGLE_H = 22f
        const val BTN_W = 130f
        const val BTN_H = 36f
        const val DIALOG_H = BTN_Y + BTN_H + 10f
    }
}
