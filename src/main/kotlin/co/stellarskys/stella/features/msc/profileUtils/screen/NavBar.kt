package co.stellarskys.stella.features.msc.profileUtils.screen

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.mc.BaseElement
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.platform.pushPop

class NavBar: BaseElement() {
    var pages: Map<ItemStack, Page> = emptyMap()
    var navigate: ((Page) -> Unit)? = null

    private val slotSize = 20
    private val gap = 2

    init {
        this.width = (pages.size * (slotSize + gap)).toFloat()
        this.height = slotSize.toFloat()
        this.x = 0f
        this.y = -(slotSize + gap).toFloat()
    }

    override fun render(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        context.pushPop {
            context.pose().translate(x, y)
            pages.entries.forEachIndexed { i, (icon, _) ->
                val slotX = i * (slotSize + gap)
                ren2d.drawRect(context, slotX, 0, slotSize, slotSize, Palette.Crust.withAlpha(150))
                ren2d.drawHollowRect(context, slotX, 0, slotSize, slotSize, 1, Palette.Purple)
                ren2d.renderItem(context, icon, slotX.toFloat() + ((slotSize / 2f) - 8f) , (slotSize / 2f) - 8f, 1f)
            }
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        val nav = navigate ?: return false
        pages.entries.forEachIndexed { i, (_, page) ->
            if (isAreaHovered(x + i * (slotSize + gap), 0f, slotSize.toFloat(), slotSize.toFloat(), mouseX, mouseY)) {
                nav(page)
                return true
            }
        }
        return false
    }
}