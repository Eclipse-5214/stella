package co.stellarskys.stella.features.dungeons.adg.editor

import co.stellarskys.stella.api.config.ui.ConfigUI
import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.dungeons.utils.DungeonClass
import co.stellarskys.stella.api.dungeons.utils.Phases
import co.stellarskys.stella.api.horizon.nvg.ParentElement
import co.stellarskys.stella.api.horizon.nvg.addTo
import co.stellarskys.stella.api.luminav2.LuminaV2
import co.stellarskys.stella.api.luminav2.types.GradientType
import co.stellarskys.stella.api.zenith.Aperture
import co.stellarskys.stella.api.zenith.Zenith
import co.stellarskys.stella.features.dungeons.adg.BossWaypoints
import co.stellarskys.stella.features.dungeons.adg.WaypointRegistry
import co.stellarskys.stella.features.secrets.utils.routes.editor.Button
import co.stellarskys.stella.utils.render.Render2D.drawLumina
import net.minecraft.client.gui.GuiGraphicsExtractor

class BossWaypointManager(
    initialPhase: Phases.F7 = Phases.currentPhase,
    initialClass: DungeonClass = BossWaypoints.dclass,
) : Aperture("Manage Boss Waypoints") {
    private val nvg get() = LuminaV2
    private val mouse = Zenith.Mouse
    private val mx get() = mouse.rawX.toFloat() / ConfigUI.UI_SCALE
    private val my get() = mouse.rawY.toFloat() / ConfigUI.UI_SCALE

    private val registry get() = WaypointRegistry.waypoints()

    private val dialogPanel = object : ParentElement() {
        override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
            width = DIALOG_W; height = DIALOG_H
            x = (rez.windowWidth / ConfigUI.UI_SCALE) / 2f - DIALOG_W / 2f
            y = (rez.windowHeight / ConfigUI.UI_SCALE) / 2f - DIALOG_H / 2f

            nvg.push()
            nvg.translate(x, y)

            nvg.rect(0f, 0f, DIALOG_W, DIALOG_H, Palette.Crust.rgb, 12f)
            nvg.hollowGradientRect(0f, 0f, DIALOG_W, DIALOG_H, 2f, Palette.Purple.rgb, Palette.Mauve.rgb, GradientType.TopLeftToBottomRight, 12f)

            nvg.text("Manage Boss Waypoints", PADDING, TITLE_Y, 18f, Palette.Text.rgb, nvg.inter)
            nvg.text("${registry.getAll().size} waypoints total", PADDING, SUBTITLE_Y, 13f, Palette.Subtext1.rgb, nvg.inter)
            nvg.rect(PADDING, DIV1_Y, CONTENT_W, 1f, Palette.Surface0.rgb)

            nvg.text("Phase", PADDING, PHASE_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)
            nvg.text("Class", PADDING, CLASS_LABEL_Y, 13f, Palette.Subtext0.rgb, nvg.inter)
            nvg.rect(PADDING, DIV2_Y, CONTENT_W, 1f, Palette.Surface0.rgb)

            elements.forEach { it.render(context, mouseX, mouseY, delta) }

            nvg.pop()
        }
    }

    private val phaseSelector = SegmentedSelector(Phases.F7.entries, initialPhase) { BossWaypointEditor.PHASE_NAMES.getValue(it) }
        .apply { width = CONTENT_W; height = SELECTOR_H; x = PADDING; y = PHASE_SELECTOR_Y }
        .addTo(dialogPanel)

    private val classSelector = SegmentedSelector(DungeonClass.valid, initialClass.takeIf { it in DungeonClass.valid } ?: DungeonClass.valid.first()) { it.displayName }
        .apply { width = CONTENT_W; height = SELECTOR_H; x = PADDING; y = CLASS_SELECTOR_Y }
        .addTo(dialogPanel)

    private val list = WaypointList(
        source = { registry.get(phaseSelector.value, classSelector.value) },
        onDelete = { delete(it) },
    ).apply { width = CONTENT_W; height = LIST_H; x = PADDING; y = LIST_Y }
        .addTo(dialogPanel)

    private val closeBtn = Button("Close", Palette.Surface1) { onClose() }
        .apply { width = BTN_W; height = BTN_H; x = DIALOG_W - PADDING - BTN_W; y = BTN_Y }
        .addTo(dialogPanel)

    private fun delete(waypoint: WaypointRegistry.Waypoint) {
        WaypointRegistry.waypoints.update { remove(phaseSelector.value, classSelector.value, waypoint) }
        BossWaypoints.waypoints = registry.get(Phases.currentPhase, BossWaypoints.dclass)
    }

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {
        context.fill(0, 0, width, height, 0x90000000.toInt())
        context.drawLumina(false) {
            nvg.push()
            nvg.scale(ConfigUI.UI_SCALE, ConfigUI.UI_SCALE)
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

    override fun onMouseScroll(x: Double, y: Double, amount: Double, horizontalAmount: Double) =
        list.mouseScrolled(mx, my, amount.toFloat(), horizontalAmount.toFloat()) || super.onMouseScroll(x, y, amount, horizontalAmount)

    override fun onKeyPress(key: Int, scanCode: Int, modifiers: Int): Boolean {
        if (key == 256) { onClose(); return true }
        return super.onKeyPress(key, scanCode, modifiers)
    }

    override fun onBackgroundRender(context: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, tickDelta: Float) {}

    companion object {
        const val DIALOG_W = 380f
        const val PADDING = 20f
        const val CONTENT_W = DIALOG_W - PADDING * 2f

        const val TITLE_Y = 20f
        const val SUBTITLE_Y = 44f
        const val DIV1_Y = 65f

        const val SELECTOR_H = 28f
        const val PHASE_LABEL_Y = 75f
        const val PHASE_SELECTOR_Y = 91f
        const val CLASS_LABEL_Y = PHASE_SELECTOR_Y + SELECTOR_H + 10f
        const val CLASS_SELECTOR_Y = CLASS_LABEL_Y + 16f
        const val DIV2_Y = CLASS_SELECTOR_Y + SELECTOR_H + 8f

        const val VISIBLE_ROWS = 6
        const val LIST_Y = DIV2_Y + 10f
        const val LIST_H = WaypointList.ROW_H * VISIBLE_ROWS

        const val BTN_W = 130f
        const val BTN_H = 36f
        const val BTN_Y = LIST_Y + LIST_H + 12f
        const val DIALOG_H = BTN_Y + BTN_H + 20f
    }
}
