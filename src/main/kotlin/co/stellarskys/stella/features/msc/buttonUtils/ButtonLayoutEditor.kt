package co.stellarskys.stella.features.msc.buttonUtils

import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.skyblock.NEUApi
import net.minecraft.client.gl.RenderPipelines
import net.minecraft.client.gui.DrawContext
import net.minecraft.util.Identifier
import xyz.meowing.vexel.core.VexelScreen
import xyz.meowing.vexel.utils.render.NVGRenderer

class ButtonLayoutEditor : VexelScreen() {
    private val dummyInventoryTexture = Identifier.of("minecraft", "container/inventory")
    private val slotSize = 20
    private val popup = EditButtonPopup(window)

    override fun afterInitialization() {
        super.afterInitialization()
    }

    override fun render(context: DrawContext?, mouseX: Int, mouseY: Int, deltaTicks: Float) {
        if (context == null) return


        // Draw dummy inventory
        val invX = (width - 176) / 2
        val invY = (height - 166) / 2

        NVGRenderer.beginFrame(width.toFloat(), height.toFloat())

        NVGRenderer.hollowRect(
            invX.toFloat(),
            invY.toFloat(),
            176f,
            166f,
            1f,
            0xFFAAAAAA.toInt(),
            4f
        )

        for (anchor in AnchorType.entries) {
            for (index in 0 until anchor.slots) {
                val (x, y) = ButtonManager.resolveAnchorPosition(anchor, index, invX, invY)

                NVGRenderer.hollowRect(
                    x.toFloat(),
                    y.toFloat(),
                    slotSize.toFloat(),
                    slotSize.toFloat(),
                    1f,
                    0xFFAAAAAA.toInt(),
                    4f
                )

                ButtonManager.getAll().find { it.anchor == anchor && it.index == index }?.let { button ->
                    if (popup.shown) return@let

                    val item = NEUApi.getItemBySkyblockId(button.iconId) ?: return@let
                    val stack = NEUApi.createDummyStack(item)

                    val offsetX = (20f - 16f) / 2f
                    val offsetY = (20f - 16f) / 2f


                    Render2D.renderItem(context,stack, x.toFloat() + offsetX, y.toFloat() + offsetY, 1f)
                }
            }
        }

        NVGRenderer.endFrame()

        super.render(context, mouseX, mouseY, deltaTicks)

        popup.renderPreviewItem(context)
    }

    override fun mouseClicked(mouseX: Double, mouseY: Double, button: Int): Boolean {
        val invX = (width - 176) / 2
        val invY = (height - 166) / 2

        if (!popup.shown) {
            for (anchor in AnchorType.entries) {
                for (index in 0 until anchor.slots) {
                    val (x, y) = ButtonManager.resolveAnchorPosition(anchor, index, invX, invY)
                    if (mouseX.toInt() in x..(x + slotSize) && mouseY.toInt() in y..(y + slotSize)) {
                        popup.open(anchor, index)
                        return super.mouseClicked(mouseX, mouseY, button)
                    }
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button)
    }

    override fun renderBackground(context: DrawContext?, mouseX: Int, mouseY: Int, deltaTicks: Float) {}
}