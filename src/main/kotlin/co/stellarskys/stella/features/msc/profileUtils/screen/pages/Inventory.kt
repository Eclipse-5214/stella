package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.ProfileViewer
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import tech.thatgravyboat.skyblockapi.utils.extentions.get
import java.awt.Color

class Inventory(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("inventory", name, navigate) {
    override val icon: ItemStack = Items.ENDER_CHEST.defaultInstance

    private enum class StorageType(val icon: () -> ItemStack) {
        INVENTORY({ Items.CHEST.defaultInstance }),
        ENDER_CHEST({ Items.ENDER_CHEST.defaultInstance }),
        BACKPACKS({ RepoItemsAPI.getItem("SMALL_BACKPACK") }),
        ACCESSORY_BAG({ Items.BOOK.defaultInstance }),
        PERSONAL_VAULT({ Items.IRON_DOOR.defaultInstance }),
        WARDROBE({ Items.LEATHER_CHESTPLATE.defaultInstance })
    }

    private var currentStorage = StorageType.INVENTORY
    private var currentPage = 0
    private var hoveredStack: ItemStack? = null

    override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        drawSidebar(context, mouseX, mouseY)
        
        if (currentStorage == StorageType.ACCESSORY_BAG) {
            val mp = "§dMP§7: §6${member.assumedMagicalPower}"
            ren2d.drawString(context, mp, 340 - client.font.width(mp), 10, 1f)
        }

        val pages = getPages()
        if (currentPage >= pages.size) currentPage = 0
        val items = pages.getOrNull(currentPage) ?: emptyList()
        
        val gridWidth = 9 * 32
        val startX = 45 + (295 - gridWidth) / 2
        
        if (currentStorage == StorageType.INVENTORY) {
            drawMainInv(context, startX, 37, mouseX, mouseY)
        } else {
            val totalHeight = ((items.size + 8) / 9) * 32
            val startY = 50 + (160 - totalHeight).coerceAtLeast(0) / 2
            drawPageBar(context, startX, pages.size, mouseX, mouseY)
            items.forEachIndexed { i, stack -> drawSlot(context, startX + (i % 9) * 32, startY + (i / 9) * 32, stack, mouseX, mouseY) }
        }
        
        hoveredStack?.let { if (!it.isEmpty) context.setTooltipForNextFrame(client.font, it, mouseX.toInt(), mouseY.toInt()) }
        hoveredStack = null 
    }

    private fun getPages(): List<List<ItemStack>> = when (currentStorage) {
        StorageType.INVENTORY -> listOf(member.inventory.invContents.items())
        StorageType.ENDER_CHEST -> member.inventory.eChestContents.items().chunked(45)
        StorageType.BACKPACKS -> (0 until 18).mapNotNull { member.inventory.backpackContents[it.toString()]?.items() }
        StorageType.ACCESSORY_BAG -> member.inventory.bags.talismanBag.items().chunked(45)
        StorageType.PERSONAL_VAULT -> member.inventory.personalVault.items().chunked(45)
        StorageType.WARDROBE -> member.inventory.wardrobeContents.items().chunked(36)
    }

    private fun drawSidebar(context: GuiGraphics, mx: Float, my: Float) = StorageType.entries.forEachIndexed { i, type ->
        val bx = 10; val by = (25 + i * 31.8f).toInt(); val selected = currentStorage == type
        if (selected) ren2d.drawRect(context, bx, by, 26, 26, Palette.Surface1.withAlpha(150))
        ren2d.drawHollowRect(context, bx, by, 26, 26, 1, Palette.Purple)
        if (isAreaHovered(bx.toFloat(), by.toFloat(), 26f, 26f, mx, my) && !selected) ren2d.drawRect(context, bx + 1, by + 1, 24, 24, Palette.Surface1.withAlpha(50))
        ren2d.renderItem(context, type.icon(), bx + 5f, by + 5f, 1f)
    }

    private fun drawPageBar(context: GuiGraphics, x: Int, total: Int, mx: Float, my: Float) {
        if (total <= 1) return
        val bw = 288 / total
        for (i in 0 until total) {
            val bx = x + (i * bw); val sel = currentPage == i
            if (sel) ren2d.drawRect(context, bx, 25, bw, 16, Palette.Surface1.withAlpha(150))
            ren2d.drawHollowRect(context, bx, 25, bw, 16, 1, Palette.Purple)
            if (isAreaHovered(bx.toFloat(), 25f, bw.toFloat(), 16f, mx, my) && !sel) ren2d.drawRect(context, bx + 1, 26, bw - 2, 14, Palette.Surface1.withAlpha(50))
            val txt = (i + 1).toString()
            ren2d.drawString(context, (if (sel) "§d" else "§7") + txt, bx + (bw / 2) - (client.font.width(txt) / 2), 29, 1f)
        }
    }

    private fun drawMainInv(ctx: GuiGraphics, x: Int, y: Int, mx: Float, my: Float) {
        val inv = member.inventory.invContents.items()
        val armor = member.inventory.invArmor.items(); val eq = member.inventory.equipment.items()
        for (i in 0 until 4) drawSlot(ctx, x + (i * 32), y, armor.getOrNull(3 - i) ?: ItemStack.EMPTY, mx, my)
        for (i in 0 until 4) drawSlot(ctx, x + (5 + i) * 32, y, eq.getOrNull(i) ?: ItemStack.EMPTY, mx, my)
        for (i in 0 until 27) drawSlot(ctx, x + (i % 9) * 32, y + 40 + (i / 9) * 32, inv.getOrNull(9 + i) ?: ItemStack.EMPTY, mx, my)
        for (i in 0 until 9) drawSlot(ctx, x + i * 32, y + 136, inv.getOrNull(i) ?: ItemStack.EMPTY, mx, my)
    }

    private fun drawSlot(ctx: GuiGraphics, ix: Int, iy: Int, stack: ItemStack, mx: Float, my: Float) {
        var bgColor = Palette.Crust.withAlpha(100)
        if (ProfileViewer.showRarity && !stack.isEmpty) {
            stack[DataTypes.RARITY]?.let { bgColor = Color(it.color).withAlpha(40) }
        }

        ren2d.drawRect(ctx, ix, iy, 30, 30, bgColor)
        ren2d.drawHollowRect(ctx, ix, iy, 30, 30, 1, Palette.Surface0)
        if (stack.isEmpty) return
        
        ren2d.renderItem(ctx, stack, ix + 3f, iy + 3f, 1.5f)

        if (isAreaHovered(ix.toFloat(), iy.toFloat(), 30f, 30f, mx, my)) {
            hoveredStack = stack
            ren2d.drawRect(ctx, ix + 1, iy + 1, 28, 28, Palette.Surface1.withAlpha(80))
        }
    }

    override fun mouseClicked(mx: Float, my: Float, button: Int): Boolean {
        if (super.mouseClicked(mx, my, button)) return true

        for (i in StorageType.entries.indices) {
            val by = 25 + i * 31.8f
            if (isAreaHovered(10f, by, 26f, 26f, mx, my)) {
                currentStorage = StorageType.entries[i]
                currentPage = 0
                return true
            }
        }
        
        val total = getPages().size
        if (total > 1 && currentStorage != StorageType.INVENTORY) {
            val bw = 288 / total
            val sx = 45f + (295f - (9 * 32)) / 2f
            for (i in 0 until total) {
                if (isAreaHovered(sx + (i * bw), 25f, bw.toFloat(), 16f, mx, my)) {
                    currentPage = i
                    return true
                }
            }
        }
        return false
    }
}
