package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.ProfileViewer
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import co.stellarskys.stella.api.horizon.mc.addTo
import co.stellarskys.stella.features.msc.profileUtils.screen.SearchBar
import co.stellarskys.stella.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.datatype.DataTypes
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockItemsRepo
import tech.thatgravyboat.skyblockapi.utils.extentions.get
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import java.awt.Color

class Storage(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
) : Page("storage", name, navigate) {
    override val icon: ItemStack = Items.ENDER_CHEST.defaultInstance

    private val subPages = listOf(NormalInventory(), EnderChest(), Backpacks(), AccessoryBag(), PersonalVault(), Wardrobe())
    private var currentPage: SubPage = subPages.first()
    private var hoveredStack: ItemStack? = null
    
    private val searchCache = java.util.IdentityHashMap<ItemStack, Boolean>()
    private val searchBar = SearchBar(230f, 6f, 110f, 14f) { searchCache.clear() }.addTo(this)

    private fun matchesSearch(stack: ItemStack): Boolean {
        if (searchBar.query.isEmpty() || stack.isEmpty) return false
        return searchCache.getOrPut(stack) {
            val q = searchBar.query.lowercase()
            val name = stack.hoverName.stripped.lowercase()
            if (name.contains(q)) true
            else stack.getLore().any { it.stripped.lowercase().contains(q) }
        }
    }

    companion object {
        const val SLOT_SIZE = 26
        const val GAP_SIZE = 2
        const val STEP_SIZE = SLOT_SIZE + GAP_SIZE
        const val GRID_ROW_COLS = 9
        const val PAGE_BAR_WIDTH = GRID_ROW_COLS * STEP_SIZE
        const val VIEW_HEIGHT_LIMIT = 160
    }

    override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float, delta: Float) {
        searchBar.render(context, mouseX, mouseY, delta)

        subPages.forEachIndexed { i, page ->
            val by = (25 + i * 31.8f).toInt()
            val isSelected = currentPage == page

            if (isSelected) ren2d.drawRect(context, 10, by, 26, 26, Palette.Surface1.withAlpha(150))
            ren2d.drawHollowRect(context, 10, by, 26, 26, 1, Palette.Purple)

            if (isAreaHovered(10f, by.toFloat(), 26f, 26f, mouseX, mouseY) && !isSelected) {
                ren2d.drawRect(context, 11, by + 1, 24, 24, Palette.Surface1.withAlpha(50))
            }
            ren2d.renderItem(context, page.icon, 15f, by + 5f, 1f)
        }
        ren2d.drawHollowRect(context, 40, 25, 300, 185, 1, Palette.Purple)
        currentPage.onRender(context, mouseX, mouseY)

        hoveredStack?.let { if (!it.isEmpty) context.setTooltipForNextFrame(client.font, it, mouseX.toInt(), mouseY.toInt()) }
        hoveredStack = null
    }

    fun renderItemGrid(context: GuiGraphics, startX: Int, startY: Int, items: List<ItemStack>, mouseX: Float, mouseY: Float) =
        items.forEachIndexed { i, stack ->
            drawSlot(context, startX + (i % GRID_ROW_COLS) * STEP_SIZE, startY + (i / GRID_ROW_COLS) * STEP_SIZE, stack, mouseX, mouseY)
        }

    private fun drawSlot(ctx: GuiGraphics, ix: Int, iy: Int, stack: ItemStack, mx: Float, my: Float) {
        var bgColor = Palette.Crust.withAlpha(100)
        var borderColor = Palette.Surface0
        var borderWidth = 1

        if (!stack.isEmpty) {
            if (ProfileViewer.showRarity) stack[DataTypes.RARITY]?.let { bgColor = Color(it.color).withAlpha(40) }
            if (searchBar.query.isNotEmpty() && matchesSearch(stack)) {
                borderColor = Palette.Purple
                borderWidth = 2
                bgColor = Palette.Purple.withAlpha(50)
            }
        }

        ren2d.drawRect(ctx, ix, iy, SLOT_SIZE, SLOT_SIZE, bgColor)
        ren2d.drawHollowRect(ctx, ix, iy, SLOT_SIZE, SLOT_SIZE, borderWidth, borderColor)
        if (stack.isEmpty) return

        val itemScale = 1.5f
        val centerOffset = (SLOT_SIZE - (16 * itemScale)) / 2f // (26 - 24) / 2 = 1.0f
        ren2d.renderItem(ctx, stack, ix + centerOffset, iy + centerOffset, itemScale)

        if (isAreaHovered(ix.toFloat(), iy.toFloat(), SLOT_SIZE.toFloat(), SLOT_SIZE.toFloat(), mx, my)) {
            hoveredStack = stack
            ren2d.drawRect(ctx, ix + 1, iy + 1, SLOT_SIZE - 2, SLOT_SIZE - 2, Palette.Surface1.withAlpha(80))
        }
    }

    private fun drawPageBar(context: GuiGraphics, startX: Int, totalPages: Int, activeIdx: Int, mx: Float, my: Float) {
        if (totalPages <= 1) return
        val bw = PAGE_BAR_WIDTH / totalPages

        for (i in 0 until totalPages) {
            val bx = startX + (i * bw)
            val isSelected = activeIdx == i

            if (isSelected) ren2d.drawRect(context, bx, 32, bw, 16, Palette.Surface1.withAlpha(150))
            ren2d.drawHollowRect(context, bx, 32, bw, 16, 1, Palette.Purple)

            if (isAreaHovered(bx.toFloat(), 32f, bw.toFloat(), 16f, mx, my) && !isSelected) {
                ren2d.drawRect(context, bx + 1, 33, bw - 2, 14, Palette.Surface1.withAlpha(50))
            }
            val txt = (i + 1).toString()
            ren2d.drawString(context, (if (isSelected) "§d" else "§7") + txt, bx + (bw / 2) - (txt.width() / 2), 36, 1f)
        }
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean {
        if (super.mouseClicked(mouseX, mouseY, button)) return true
        subPages.forEachIndexed { i, page ->
            if (isAreaHovered(10f, 25 + i * 31.8f, 26f, 26f, mouseX, mouseY)) {
                currentPage = page; return true
            }
        }
        return currentPage.mouseClicked(mouseX, mouseY, button)
    }

    abstract class SubPage(val name: String) {
        abstract val icon: ItemStack
        protected var pageIdx = 0
        open fun resetPage() { pageIdx = 0 }
        abstract fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float)
        open fun mouseClicked(mx: Float, my: Float, button: Int): Boolean = false
        protected val startX: Int = 45 + (300 - PAGE_BAR_WIDTH) / 2
    }

    abstract inner class PagedSubPage(name: String) : SubPage(name) {
        abstract val cachedPages: List<List<ItemStack>>

        override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float) {
            val pages = cachedPages
            drawPageBar(context, startX, pages.size, pageIdx, mouseX, mouseY)

            val currentItems = pages.getOrNull(pageIdx) ?: emptyList()
            val startY = 50 + (VIEW_HEIGHT_LIMIT - (((currentItems.size + 8) / GRID_ROW_COLS) * STEP_SIZE)).coerceAtLeast(0) / 2
            renderItemGrid(context, startX, startY, currentItems, mouseX, mouseY)
        }

        override fun mouseClicked(mx: Float, my: Float, button: Int): Boolean {
            val total = cachedPages.size
            if (total <= 1) return false
            val bw = PAGE_BAR_WIDTH / total
            for (i in 0 until total) {
                if (isAreaHovered(startX + (i * bw).toFloat(), 32f, bw.toFloat(), 16f, mx, my)) {
                    pageIdx = i
                    return true
                }
            }
            return false
        }
    }

    inner class NormalInventory : SubPage("Inventory") {
        override val icon: ItemStack = Items.CHEST.defaultInstance
        private val cachedData by lazy {
            val armor = member.inventory.invArmor.items().reversed()
            val eq = member.inventory.equipment.items()
            val allInv = member.inventory.invContents.items()
            val hotbar = allInv.take(9)
            val inv = allInv.drop(9)
            Triple(armor, eq, hotbar to inv)
        }

        override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float) {
            val (armor, eq, invHotbar) = cachedData
            renderItemGrid(context, startX, 37, armor, mouseX, mouseY)
            renderItemGrid(context, startX + (5 * STEP_SIZE), 37, eq, mouseX, mouseY)
            renderItemGrid(context, startX, 77, invHotbar.second, mouseX, mouseY)
            renderItemGrid(context, startX, 173, invHotbar.first, mouseX, mouseY)
        }
    }

    inner class PersonalVault : SubPage("Personal Vault") {
        override val icon: ItemStack = Items.IRON_DOOR.defaultInstance
        private val cachedItems by lazy { member.inventory.personalVault.items() }

        override fun onRender(context: GuiGraphics, mouseX: Float, mouseY: Float) {
            val items = cachedItems
            val startY = 35 + (VIEW_HEIGHT_LIMIT - (((items.size + 8) / GRID_ROW_COLS) * STEP_SIZE)).coerceAtLeast(0) / 2
            renderItemGrid(context, startX, startY, items, mouseX, mouseY)
        }
    }

    inner class EnderChest : PagedSubPage("Ender Chest") {
        override val icon: ItemStack = Items.ENDER_CHEST.defaultInstance
        override val cachedPages by lazy { member.inventory.enderChestPages }
    }

    inner class Backpacks : PagedSubPage("Backpacks") {
        override val icon: ItemStack = SkyBlockItemsRepo.getItemStackOrDefault("SMALL_BACKPACK")
        override val cachedPages by lazy { (0 until 18).mapNotNull { member.inventory.backpackContents[it.toString()]?.items() } }
    }

    inner class AccessoryBag : PagedSubPage("Accessory Bag") {
        override val icon: ItemStack = Items.BOOK.defaultInstance
        override val cachedPages by lazy { member.inventory.bags.accessoryBagPages }
    }

    inner class Wardrobe : PagedSubPage("Wardrobe") {
        override val icon: ItemStack = Items.LEATHER_CHESTPLATE.defaultInstance
        override val cachedPages by lazy { member.inventory.fullWardrobe.chunked(36) }
    }
}