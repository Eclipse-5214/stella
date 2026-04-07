package co.stellarskys.stella.api.rrv.crafting

import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object StellaCraftingRecipeType : ReliableClientRecipeType {
    private const val SLOT = 18
    private const val OUTPUT_X = 94
    private const val OUTPUT_Y = 18

    private val iconStack = ItemStack(Items.CRAFTING_TABLE)
    private val craftRefs = listOf(iconStack)

    override fun getDisplayName(): Component = Component.literal("SkyBlock Crafting")

    // SBE uses slightly larger bounds to fit the wiki button (118x68)
    override fun getDisplayWidth(): Int = 118
    override fun getDisplayHeight(): Int = 68
    override fun getGuiTexture(): Identifier? = null // Null = Default RRV gray background
    override fun getSlotCount(): Int = 10

    override fun placeSlots(def: RecipeViewMenu.SlotDefinition) {
        // 3x3 Grid
        for (row in 0 until 3) {
            for (col in 0 until 3) {
                def.addItemSlot(row * 3 + col, col * SLOT, row * SLOT)
            }
        }
        // Result Slot
        def.addItemSlot(9, OUTPUT_X, OUTPUT_Y)
    }

    override fun getId(): Identifier = Identifier.fromNamespaceAndPath("stella", "skyblock_crafting")

    override fun getIcon(): ItemStack = iconStack

    override fun getCraftReferences(): List<ItemStack> = craftRefs
}