package co.stellarskys.stella.api.rrv.crafting

import cc.cassian.rrv.api.recipe.ReliableClientRecipe
import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.api.rrv.RrvCompat
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import java.awt.Color

class StellaCraftingClientRecipe(
    private val inputs: Array<SlotContent?>,
    private val output: SlotContent?,
) : ReliableClientRecipe {
    override fun getViewType(): ReliableClientRecipeType = StellaCraftingRecipeType
    override fun getIngredients(): List<SlotContent> = inputs.filterNotNull()
    override fun getResults(): List<SlotContent> = output?.let { listOf(it) } ?: emptyList()
    override fun redirectsAsResult(stack: ItemStack?) = RrvCompat.checkIngredient(stack, results)
    override fun redirectsAsIngredient(stack: ItemStack?) = RrvCompat.checkIngredient(stack, ingredients)

    override fun bindSlots(ctx: RecipeViewMenu.SlotFillContext) {
        inputs.forEachIndexed { i, slot ->
            slot?.let { ctx.bindSlot(i, it) }
        }
        output?.let { ctx.bindSlot(9, it) }
    }

    override fun renderRecipe(screen: RecipeViewScreen, pos: ReliableClientRecipe.RecipePosition, guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        Render2D.drawString(guiGraphics, "→" , 62, 22, color = Color.GRAY, shadow = false)
    }
}