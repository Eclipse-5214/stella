package co.stellarskys.stella.features.dungeons.adg.editor

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.horizon.nvg.BaseElement
import co.stellarskys.stella.features.dungeons.adg.WaypointRegistry.Waypoint
import net.minecraft.client.gui.GuiGraphicsExtractor

class WaypointList(
    private val source: () -> List<Waypoint>,
    private val onDelete: (Waypoint) -> Unit,
) : BaseElement() {
    private var scroll = 0f

    private val maxScroll get() = (source().size * ROW_H - height).coerceAtLeast(0f)
    private val deleteX get() = width - DELETE_W - 8f

    override fun render(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        val entries = source()
        scroll = scroll.coerceIn(0f, maxScroll)
        val listHovered = isAreaHovered(0f, 0f, width, height)

        nvg.push()
        nvg.translate(x, y)
        nvg.rect(0f, 0f, width, height, Palette.Base.rgb, 8f)

        if (entries.isEmpty()) {
            val text = "No waypoints for this phase and class"
            val textWidth = nvg.textWidth(text, 13f, nvg.inter)
            nvg.text(text, width / 2f - textWidth / 2f, height / 2f - 6.5f, 13f, Palette.Subtext0.rgb, nvg.inter)
        }

        nvg.pushScissor(0f, 0f, width, height)
        entries.forEachIndexed { index, waypoint ->
            val rowY = index * ROW_H - scroll
            if (rowY + ROW_H < 0f || rowY > height) return@forEachIndexed

            if (listHovered && isAreaHovered(0f, rowY, width, ROW_H)) {
                nvg.rect(2f, rowY + 2f, width - 4f, ROW_H - 4f, Palette.Surface0.rgb, 6f)
            }

            val textY = rowY + ROW_H / 2f - 6.5f
            nvg.rect(10f, rowY + ROW_H / 2f - 6f, 12f, 12f, waypoint.color.rgb, 3f)

            val position = "(${waypoint.pos.x}, ${waypoint.pos.y}, ${waypoint.pos.z})"
            nvg.text(position, 30f, textY, 13f, Palette.Text.rgb, nvg.inter)

            waypoint.label?.let { label ->
                val labelX = 30f + nvg.textWidth(position, 13f, nvg.inter) + 10f
                nvg.pushScissor(labelX, rowY, deleteX - labelX - 6f, ROW_H)
                nvg.text("\"$label\"", labelX, textY, 13f, Palette.Subtext1.rgb, nvg.inter)
                nvg.popScissor()
            }

            val deleteY = rowY + (ROW_H - DELETE_H) / 2f
            val deleteHovered = listHovered && isAreaHovered(deleteX, deleteY, DELETE_W, DELETE_H)
            nvg.rect(deleteX, deleteY, DELETE_W, DELETE_H, (if (deleteHovered) Palette.Red else Palette.Surface1).rgb, 6f)
            val deleteTextWidth = nvg.textWidth("Delete", 12f, nvg.inter)
            nvg.text("Delete", deleteX + DELETE_W / 2f - deleteTextWidth / 2f, deleteY + DELETE_H / 2f - 6f, 12f, Palette.Text.rgb, nvg.inter)
        }
        nvg.popScissor()

        nvg.pop()
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (!isAreaHovered(0f, 0f, width, height)) return false

        val entries = source()
        val index = ((mouseY - absoluteY + scroll) / ROW_H).toInt()
        val waypoint = entries.getOrNull(index) ?: return true

        val deleteY = index * ROW_H - scroll + (ROW_H - DELETE_H) / 2f
        if (isAreaHovered(deleteX, deleteY, DELETE_W, DELETE_H)) onDelete(waypoint)
        return true
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float, horizontalAmount: Float): Boolean {
        if (!isAreaHovered(0f, 0f, width, height)) return false
        scroll = (scroll - amount * ROW_H).coerceIn(0f, maxScroll)
        return true
    }

    companion object {
        const val ROW_H = 32f
        const val DELETE_W = 56f
        const val DELETE_H = 22f
    }
}
