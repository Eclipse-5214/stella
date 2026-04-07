package co.stellarskys.stella.api.rrv.forge

import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items

object StellaForgeRecipeType : ReliableClientRecipeType {
    override fun getDisplayName(): Component = Component.literal("The Forge")
    override fun getDisplayWidth(): Int = 140
    override fun getDisplayHeight(): Int = 75
    override fun getGuiTexture(): Identifier? = null
    override fun getSlotCount(): Int = 9

    override fun placeSlots(def: RecipeViewMenu.SlotDefinition) {
        val startX = (140 - (7 * 18)) / 2
        for (i in 0 until 7) {
            def.addItemSlot(i, startX + (i * 18), 10) // Keep inputs at Y=10
        }
        def.addItemSlot(8, (140 - 18) / 2, 35)
    }

    override fun getId(): Identifier = Identifier.fromNamespaceAndPath("stella", "forge")
    override fun getIcon(): ItemStack = ItemStack(Items.ANVIL)
}