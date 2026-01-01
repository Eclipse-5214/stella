package co.stellarskys.stella.features.msc.buttonUtils

import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.vexel.Vexel
import co.stellarskys.vexel.core.VexelScreen
import dev.deftu.omnicore.api.client.input.KeyboardModifiers
import dev.deftu.omnicore.api.client.input.OmniMouseButton
import dev.deftu.omnicore.api.client.render.OmniRenderingContext
import dev.deftu.omnicore.api.client.render.OmniResolution
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI

class ButtonLayoutEditor : VexelScreen() {
    private val slotSize = 20
    private val popup = EditButtonPopup(window)

    override fun onRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {
        val context = ctx.graphics ?: return
        context.fill(0, 0, width, height, 0x90000000.toInt())

        // Draw dummy inventory
        val invX = (width - 176) / 2
        val invY = (height - 166) / 2

        context.drawNVG {
            Vexel.renderer.hollowRect(
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

                    Vexel.renderer.hollowRect(
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
                        val stack = RepoItemsAPI.getItem(button.iconId)

                        val offsetX = (20f - 16f) / 2f
                        val offsetY = (20f - 16f) / 2f


                        Render2D.renderItem(context, stack, x.toFloat() + offsetX, y.toFloat() + offsetY, 1f)
                    }
                }
            }
        }

        super.onRender(ctx, mouseX, mouseY, tickDelta)
        popup.renderPreviewItem(context)
    }

    override fun onMouseClick(button: OmniMouseButton, x: Double, y: Double, modifiers: KeyboardModifiers): Boolean {
        val invX = (width - 176) / 2
        val invY = (height - 166) / 2

        if (!popup.shown) {
            for (anchor in AnchorType.entries) {
                for (index in 0 until anchor.slots) {
                    val (bx, by) = ButtonManager.resolveAnchorPosition(anchor, index, invX, invY)
                    if (x.toInt() in bx..(bx + slotSize) && y.toInt() in by..(by + slotSize)) {
                        popup.open(anchor, index)
                        return super.onMouseClick(button, x, y, modifiers)
                    }
                }
            }
        }

        return super.onMouseClick(button, x, y, modifiers)
    }

    override fun onBackgroundRender(ctx: OmniRenderingContext, mouseX: Int, mouseY: Int, tickDelta: Float) {}
}