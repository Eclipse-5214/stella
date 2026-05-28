package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.features.msc.profileUtils.CollectionUtils
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import co.stellarskys.stella.utils.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.platform.pushPop

class Collection(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("collection", name, navigate) {
    override val icon: ItemStack = Items.PAINTING.defaultInstance

    private var currentCategory = CollectionUtils.CollectionType.FARMING
    private var scrollOffset by Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var targetOffset = 0f

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        drawSidebar(context, mouseX, mouseY)
        
        if (!CollectionUtils.isLoaded()) {
            ren2d.drawString(context, "§bLoading API resources...", 115, 35, 1f)
            return
        }

        val allItems = CollectionUtils.getCategoryProgress(currentCategory, member)
        val totalHeight = (allItems.size / 2 + 1) * 31

        ren2d.renderScrolled(context, 105, 25, 235, 185, scrollOffset) {
            allItems.forEachIndexed { i, item ->
                val ix = (i % 2) * 118; val iy = (i / 2) * 31
                val locked = item.amount == 0L
                val progress = if (item.nextTierAmount == 0L) 1.0f else item.amount.toFloat() / item.nextTierAmount.toFloat()
                
                ren2d.drawHollowRect(context, ix, iy, 110, 25, 1, if (locked) Palette.Surface0 else Palette.Purple)
                ren2d.renderItem(context, CollectionUtils.getIcon(item.id), ix + 5f, iy + 5f, 1f)
                
                val title = Component.literal("${if (locked) "§8" else "§a"}${item.name}").onHover("§b${item.name}\n§bTier: §6${item.currentTier} / ${item.maxTier}\n§fAmount: §7${"%,d".format(item.amount)}")
                
                val sy = iy + scrollOffset
                if (sy >= -25 && sy < 185) context.pushPop {
                    context.pose().translate(-105f, -(25f + scrollOffset))
                    drawComp(context, title, 105 + ix + 25, 25 + iy + 5 + scrollOffset.toInt())
                } else ren2d.drawString(context, title, ix + 25, iy + 5)
                
                ren2d.drawRect(context, ix + 25, iy + 15, 80, 5, Palette.Crust)
                if (!locked) ren2d.drawRect(context, ix + 25, iy + 15, (80 * progress.coerceAtMost(1f)).toInt(), 5, if (item.currentTier == item.maxTier) Palette.Sapphire else Palette.Green)
            }
        }

        ren2d.drawScrollbar(context, 338, 25, 185, scrollOffset, totalHeight, Palette.Purple)
    }

    private fun drawSidebar(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float) {
        ren2d.drawHollowRect(context, 10, 25, 90, 185, 1, Palette.Purple)
        CollectionUtils.CollectionType.entries.forEachIndexed { i, cat ->
            val bx = 12; val by = 27 + (i * 26); val sel = currentCategory == cat
            if (sel) {
                ren2d.drawRect(context, bx, by, 86, 25, Palette.Surface1)
                ren2d.drawHollowRect(context, bx, by, 86, 25, 1, Palette.Purple)
            } else if (isAreaHovered(bx.toFloat(), by.toFloat(), 86f, 25f, mouseX, mouseY)) ren2d.drawRect(context, bx, by, 86, 25, Palette.Surface0)
            
            ren2d.renderItem(context, cat.icon(), bx + 4f, by + 4f, 1f)
            ren2d.drawString(context, (if (sel) "§d" else "§7") + cat.displayName, bx + 24, by + 8, 1f)
        }
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float, horizontalAmount: Float): Boolean {
        if (!isAreaHovered(105f, 25f, 235f, 185f, mouseX, mouseY)) return false
        val total = (CollectionUtils.getCategoryProgress(currentCategory, member).size / 2 + 1) * 31
        targetOffset = ren2d.calculateScroll(targetOffset, amount, total, 185)
        scrollOffset = targetOffset
        return true
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (super.mouseClicked(mouseX, mouseY, button)) return true
        CollectionUtils.CollectionType.entries.forEachIndexed { i, cat ->
            if (isAreaHovered(12f, 27f + (i * 26), 86f, 25f, mouseX, mouseY)) {
                if (currentCategory != cat) { currentCategory = cat; targetOffset = 0f; scrollOffset = 0f }
                return true
            }
        }
        return false
    }
}
