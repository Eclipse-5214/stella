package co.stellarskys.stella.features.dungeons.adg.editor

import co.stellarskys.stella.api.config.ui.ConfigUI
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.dungeons.utils.Phases
import co.stellarskys.stella.api.horizon.nvg.ParentElement
import co.stellarskys.stella.api.horizon.nvg.TextBox
import co.stellarskys.stella.api.horizon.nvg.addTo
import co.stellarskys.stella.api.luminav2.LuminaV2
import co.stellarskys.stella.api.luminav2.types.GradientType
import co.stellarskys.stella.api.zenith.Aperture
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.features.dungeons.adg.BossWaypoints
import co.stellarskys.stella.features.dungeons.adg.WaypointRegistry
import co.stellarskys.stella.features.secrets.utils.routes.editor.Button
import co.stellarskys.stella.features.secrets.utils.routes.editor.ColorPicker
import co.stellarskys.stella.features.secrets.utils.routes.editor.Toggle
import co.stellarskys.stella.utils.render.Render2D.drawLumina
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.core.BlockPos

class BossWaypointEditor(
    val pos: BlockPos,
    initialPhase: Phases.F7,
    initialClass: DungeonClass,
) : Aperture("Add Boss Waypoint") {
    private val nvg get() = LuminaV2
    private val mouse = Zenith.Mouse
    private val mx get() = mouse.rawX.toFloat() / ConfigUI.UI_SCALE
    private val my get() = mouse.rawY.toFloat() / ConfigUI.UI_SCALE

    private val dialogPanel = object : ParentElement() {
        override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
            width = DIALOG_W; height = DIALOG_H
            x = (rez.windowWidth / ConfigUI.UI_SCALE) / 2f - DIALOG_W / 2f
            y = (rez.windowHeight / ConfigUI.UI_SCALE) / 2f - DIALOG_H / 2f

            nvg.push()
            nvg.translate(x, y)

            nvg.rect(0f, 0f, DIALOG_W, DIALOG_H, Palette.Crust.rgb, 12f)
            nvg.hollowGradientRect(0f, 0f, DIALOG_W, DIALOG_H, 2f, Palette.Purple.rgb, Palette.Mauve.rgb, GradientType.TopLeftToBottomRight, 12f)

            nvg.text("Add Boss Waypoint", PADDING, TITLE_Y, 18f, Palette.Text.rgb, nvg.inter)
            nvg.text("Position: (${pos.x}, ${pos.y}, ${pos.z})", PADDING, POS_Y, 13f, Palette.Subtext1.rgb, nvg.inter)
            nvg.rect(PADDING, DIV1_Y, CONTENT_W, 1f, Palette.Surface0.rgb)

            nvg.text("Phase", PADDING, PHASE_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)
            nvg.text("Class", PADDING, CLASS_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)
            nvg.rect(PADDING, DIV2_Y, CONTENT_W, 1f, Palette.Surface0.rgb)

            nvg.text("Label", PADDING, LABEL_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)
            nvg.rect(PADDING, DIV3_Y, CONTENT_W, 1f, Palette.Surface0.rgb)

            nvg.text("Color", PADDING, COLOR_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)

            nvg.rect(PADDING, DEPTH_SECTION_Y, CONTENT_W, 1f, Palette.Surface0.rgb)
            nvg.text("Depth Check", PADDING, DEPTH_SECTION_Y + 12f, 13f, Palette.Subtext0.rgb, nvg.inter)

            nvg.rect(PADDING, ABOVE_SECTION_Y, CONTENT_W, 1f, Palette.Surface0.rgb)
            nvg.text("Label Above Block", PADDING, ABOVE_SECTION_Y + 12f, 13f, Palette.Subtext0.rgb, nvg.inter)

            elements.forEach { it.render(context, mouseX, mouseY, delta) }

            nvg.pop()
        }
    }

    private val phaseSelector = SegmentedSelector(Phases.F7.entries, initialPhase) { PHASE_NAMES.getValue(it) }
        .apply { width = CONTENT_W; height = SELECTOR_H; x = PADDING; y = PHASE_SELECTOR_Y }
        .addTo(dialogPanel)

    private val classSelector = SegmentedSelector(DungeonClass.valid, initialClass.takeIf { it in DungeonClass.valid } ?: DungeonClass.valid.first()) { it.displayName }
        .apply { width = CONTENT_W; height = SELECTOR_H; x = PADDING; y = CLASS_SELECTOR_Y }
        .addTo(dialogPanel)

    private var label = ""
    private val labelBox: TextBox = TextBox(
        PADDING, LABEL_BOX_Y, CONTENT_W, LABEL_BOX_H,
        initialText = "",
        color = Palette.Base.rgb,
        borderColor = Palette.Purple.withAlpha(50).rgb,
        focusColor = Palette.Purple.rgb,
        maxLength = 32,
        onType = { label = it }
    ).addTo(dialogPanel)

    private val colorPicker = ColorPicker(CONTENT_W)
        .apply { x = PADDING; y = COLOR_PICKER_Y }
        .addTo(dialogPanel)

    private val depthToggle = Toggle(true)
        .apply { width = TOGGLE_W; height = TOGGLE_H; x = DIALOG_W - PADDING - TOGGLE_W; y = TOGGLE_Y }
        .addTo(dialogPanel)

    private val aboveToggle = Toggle(false)
        .apply { width = TOGGLE_W; height = TOGGLE_H; x = DIALOG_W - PADDING - TOGGLE_W; y = ABOVE_TOGGLE_Y }
        .addTo(dialogPanel)

    private val cancelBtn = Button("Cancel", Palette.Surface1) { onClose() }
        .apply { width = BTN_W; height = BTN_H; x = PADDING; y = BTN_Y }
        .addTo(dialogPanel)

    private val confirmBtn = Button("Add Waypoint", Palette.Purple) { confirm() }
        .apply { width = BTN_W; height = BTN_H; x = DIALOG_W - PADDING - BTN_W; y = BTN_Y }
        .addTo(dialogPanel)

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        context.fill(0, 0, width, height, 0x90000000.toInt())
        context.drawLumina(false) {
            nvg.push()
            nvg.scale(ConfigUI.Companion.UI_SCALE, ConfigUI.Companion.UI_SCALE)
            dialogPanel.render(context, mx, my, tickDelta)
            nvg.pop()
        }
    }

    override fun onMouseClick(button: Int, x: Double, y: Double, modifiers: Int): Boolean {
        labelBox.isFocused = false
        colorPicker.hexBox.isFocused = false
        return dialogPanel.mouseClicked(mx, my, button) || super.onMouseClick(button, x, y, modifiers)
    }

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
        val waypoint = WaypointRegistry.Waypoint(
            pos = pos,
            color = colorPicker.currentColor,
            depth = depthToggle.value,
            above = aboveToggle.value,
            label = label.ifBlank { null },
        )
        WaypointRegistry.waypoints.update { add(phaseSelector.value, classSelector.value, waypoint) }
        BossWaypoints.waypoints = WaypointRegistry.waypoints().get(Phases.currentPhase, BossWaypoints.dclass)
        onClose()
    }

    override fun onBackgroundRender(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {}

    companion object {
        val PHASE_NAMES = mapOf(
            Phases.F7.P1 to "Maxor",
            Phases.F7.P2 to "Storm",
            Phases.F7.P3 to "Goldor",
            Phases.F7.P4 to "Necron",
            Phases.F7.P5 to "King",
        )

        const val DIALOG_W = 380f
        const val PADDING = 20f
        const val CONTENT_W = DIALOG_W - PADDING * 2f

        const val TITLE_Y = 20f
        const val POS_Y = 44f
        const val DIV1_Y = 65f

        const val SELECTOR_H = 28f
        const val PHASE_LABEL_Y = 75f
        const val PHASE_SELECTOR_Y = 91f
        const val CLASS_LABEL_Y = PHASE_SELECTOR_Y + SELECTOR_H + 10f
        const val CLASS_SELECTOR_Y = CLASS_LABEL_Y + 16f
        const val DIV2_Y = CLASS_SELECTOR_Y + SELECTOR_H + 8f

        const val LABEL_LABEL_Y = DIV2_Y + 10f
        const val LABEL_BOX_Y = LABEL_LABEL_Y + 16f
        const val LABEL_BOX_H = 28f
        const val DIV3_Y = LABEL_BOX_Y + LABEL_BOX_H + 8f

        const val COLOR_LABEL_Y = DIV3_Y + 10f
        const val COLOR_PICKER_Y = COLOR_LABEL_Y + 18f

        const val DEPTH_SECTION_Y = COLOR_PICKER_Y + ColorPicker.PICKER_HEIGHT + 8f
        const val TOGGLE_Y = DEPTH_SECTION_Y + 8f

        const val ABOVE_SECTION_Y = DEPTH_SECTION_Y + 38f
        const val ABOVE_TOGGLE_Y = ABOVE_SECTION_Y + 8f

        const val BTN_Y = ABOVE_SECTION_Y + 46f

        const val TOGGLE_W = 42f
        const val TOGGLE_H = 22f
        const val BTN_W = 130f
        const val BTN_H = 36f
        const val DIALOG_H = BTN_Y + BTN_H + 10f
    }
}
