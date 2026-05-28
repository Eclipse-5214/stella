package co.stellarskys.stella.features.msc.profileUtils.screen.pages

import co.stellarskys.stella.api.config.ui.Palette
import co.stellarskys.stella.api.config.ui.Palette.withAlpha
import co.stellarskys.stella.api.horizon.animation.AnimType
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.features.msc.ProfileViewer
import co.stellarskys.stella.features.msc.profileUtils.PetUtils
import co.stellarskys.stella.features.msc.profileUtils.PetUtils.item
import co.stellarskys.stella.features.msc.profileUtils.screen.Page
import co.stellarskys.stella.utils.Utils
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockItemsRepo
import tech.thatgravyboat.skyblockapi.platform.pushPop
import java.awt.Color

class Pets(
    name: String,
    val member: SkyblockResponse.SkyblockMember,
    navigate: (Page) -> Unit
): Page("pets", name, navigate) {
    private val sortedPets by lazy {
        member.petsData.pets.sortedWith(
            compareByDescending<SkyblockResponse.Pet> { it.rarity }
                .thenByDescending { it.exp }
        )
    }

    private val totalRows by lazy { (sortedPets.size / GRID_ROW_COLS + 1) }
    private val totalHeight by lazy { totalRows * STEP_SIZE + 10 }
    private var selectedPet = member.petsData.activePet ?: sortedPets.firstOrNull()
    private var hoveredPet: ItemStack? = null
    private var scrollOffset by Utils.animate<Float>(0.2, AnimType.EASE_OUT)
    private var targetOffset = 0f

    override val icon: ItemStack = Items.BONE.defaultInstance

    companion object {
        const val SLOT_SIZE = 26
        const val GAP_SIZE = 2
        const val STEP_SIZE = SLOT_SIZE + GAP_SIZE
        const val GRID_ROW_COLS = 6
        const val PREVIEW_WIDTH = 130
        const val BAR_WIDTH = 90
    }

    override fun onRender(context: GuiGraphicsExtractor, mouseX: Float, mouseY: Float, delta: Float) {
        // Pets Scroller
        ren2d.drawHollowRect(context, 10, 25, 190, 185, 1, Palette.Purple)
        renderPets(context, 15, 30, mouseX, mouseY)
        ren2d.drawScrollbar(context, 193, 30, 175, scrollOffset, totalHeight, Palette.Purple)

        // Pet Preview
        ren2d.drawHollowRect(context, 210, 25, PREVIEW_WIDTH, 185, 1, Palette.Purple)
        renderPetOverview(context, 210, 25)

        hoveredPet?.let { if (!it.isEmpty) context.setTooltipForNextFrame(client.font, it, mouseX.toInt(), mouseY.toInt()) }
        hoveredPet = null
    }

    private fun renderPets(context: GuiGraphicsExtractor, x: Int, y: Int, mouseX: Float, mouseY: Float) {
        ren2d.renderScrolled(context, x, y, 190, 175, scrollOffset) {
            renderPetsGrid(context, 8, 0, x, y, sortedPets, mouseX, mouseY)
        }
    }

    private fun renderPetOverview(context: GuiGraphicsExtractor, x: Int, y: Int) {
        val pet = selectedPet ?: run {
            val noPetText = "§cNo Selected Pet"
            val textX = x + (PREVIEW_WIDTH - client.font.width(noPetText)) / 2
            ren2d.drawString(context, noPetText, textX, y + 15)
            return
        }

        val petLevel = PetUtils.getPetLevel(pet)
        val itemScale = 3f
        val itemX = x + (PREVIEW_WIDTH / 2f)
        val barX = x + (PREVIEW_WIDTH - BAR_WIDTH) / 2

        context.pushPop {
            context.pose().translate(itemX, y + 15f)
            context.pose().scale(itemScale)
            context.item(pet.item(), -8, 0)
        }

        with(petLevel) {
            val petName = pet.type.lowercase().split("_")
                .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

            val nameComp = Component.literal("§7[Lvl $level] ")
                .append(Component.literal(petName).withColor(pet.rarity.color))

            val nameX = x + (PREVIEW_WIDTH - client.font.width(nameComp)) / 2
            ren2d.drawString(context, nameComp, nameX, y + 70)
            drawBar(context, barX, y + 82, (level.toFloat() / levelCap.toFloat()).coerceIn(0f, 1f))

            val nextLvlText = "§dTo Next §6${(progressToNextLevel * 100f).toInt()}%"
            val nextLvlX = x + (PREVIEW_WIDTH - client.font.width(nextLvlText)) / 2
            ren2d.drawString(context, nextLvlText, nextLvlX, y + 100)
            drawBar(context, barX, y + 112, progressToNextLevel)

            val maxLvlText = "§dTo Max §6${(progressToMax * 100f).toInt()}%"
            val maxLvlX = x + (PREVIEW_WIDTH - client.font.width(maxLvlText)) / 2
            ren2d.drawString(context, maxLvlText, maxLvlX, y + 130)
            drawBar(context, barX, y + 142, progressToMax)
        }

        pet.heldItem?.let {
            val stack = SkyBlockItemsRepo.getItemStackOrDefault(it)
            ren2d.renderItem(context, stack, x + 75f, y + 45f, 1f)
            if (isAreaHovered(x + 75f, y + 45f, 16f, 16f)) hoveredPet = stack
        }
    }

    private fun renderPetsGrid(context: GuiGraphicsExtractor, sx: Int, sy: Int, ox: Int, oy: Int, pets: List<SkyblockResponse.Pet>, mouseX: Float, mouseY: Float) =
        pets.forEachIndexed { i, stack ->
            drawPet(context, sx + (i % GRID_ROW_COLS) * STEP_SIZE, sy + (i / GRID_ROW_COLS) * STEP_SIZE, ox, oy, stack, mouseX, mouseY)
        }

    private fun drawPet(ctx: GuiGraphicsExtractor, ix: Int, iy: Int, ox: Int, oy: Int, pet: SkyblockResponse.Pet, mx: Float, my: Float) {
        val bgColor = Color(pet.rarity.color).withAlpha(40)
        val petItem = pet.item()

        ren2d.drawRect(ctx, ix, iy, SLOT_SIZE, SLOT_SIZE, bgColor)
        ren2d.drawHollowRect(ctx, ix, iy, SLOT_SIZE, SLOT_SIZE, 1, if (pet == selectedPet) Palette.Green else Palette.Surface0)

        val itemScale = 1.5f
        val centerOffset = (SLOT_SIZE - (16 * itemScale)) / 2f
        ren2d.renderItem(ctx, petItem, ix + centerOffset, iy + centerOffset, itemScale)

        if (isAreaHovered((ix + ox).toFloat(), (iy + oy).toFloat() + scrollOffset, SLOT_SIZE.toFloat(), SLOT_SIZE.toFloat(), mx, my)) {
            hoveredPet = petItem
            ren2d.drawRect(ctx, ix + 1, iy + 1, SLOT_SIZE - 2, SLOT_SIZE - 2, Palette.Surface1.withAlpha(80))
        }
    }

    private fun drawBar(context: GuiGraphicsExtractor, x: Int, y: Int, filledPercent: Float) {
        ren2d.drawRect(context, x, y, BAR_WIDTH, 5, Palette.Crust)
        ren2d.drawRect(context, x, y, (BAR_WIDTH * filledPercent).toInt(), 5, Palette.Green)
    }

    override fun mouseScrolled(mouseX: Float, mouseY: Float, amount: Float, horizontalAmount: Float): Boolean {
        if (!isAreaHovered(10f, 25f, 200f, 185f, mouseX, mouseY)) return false
        targetOffset = ren2d.calculateScroll(targetOffset, amount, totalHeight, 175)
        scrollOffset = targetOffset
        return true
    }

    override fun mouseClicked(mouseX: Float, mouseY: Float, button: Int): Boolean =
        super.mouseClicked(mouseX, mouseY, button) || isAreaHovered(15f, 30f, 190f, 175f, mouseX, mouseY).let { hovered ->
            if (!hovered) return false
            val lx = (mouseX - absoluteX - 23).toInt()
            val ly = (mouseY - absoluteY - 30 - scrollOffset).toInt()

            val col = Math.floorDiv(lx, STEP_SIZE)
            val row = Math.floorDiv(ly, STEP_SIZE)

            (col in 0 until GRID_ROW_COLS && lx.mod(STEP_SIZE) <= SLOT_SIZE && ly.mod(STEP_SIZE) <= SLOT_SIZE)
                .let { insideSlot -> (row * GRID_ROW_COLS + col).takeIf { insideSlot && it in sortedPets.indices } }
                ?.let { selectedPet = sortedPets[it]; true } ?: false
        }
}