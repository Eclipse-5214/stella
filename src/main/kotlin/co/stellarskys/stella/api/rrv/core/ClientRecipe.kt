package co.stellarskys.stella.api.rrv.core

import cc.cassian.rrv.api.recipe.ReliableClientRecipe
import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.api.rrv.RrvCompat
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack

class ClientRecipe(
    private val server: ServerRecipe,
    private val viewType: ReliableClientRecipeType,
    private val binder: (RecipeViewMenu.SlotFillContext, ServerRecipe) -> Unit,
    private val renderer: (GuiGraphicsExtractor, ServerRecipe) -> Unit,
    private val resultMatcher: (ItemStack?, List<SlotContent>) -> Boolean,
    private val ingredientMatcher: (ItemStack?, List<SlotContent>) -> Boolean
) : ReliableClientRecipe {

    override fun getViewType() = viewType
    override fun getIngredients() = server.ingredients
    override fun getResults() = server.results

    override fun redirectsAsResult(stack: ItemStack?) = resultMatcher(stack, results)
    override fun redirectsAsIngredient(stack: ItemStack?) = ingredientMatcher(stack, ingredients)

    override fun bindSlots(ctx: RecipeViewMenu.SlotFillContext) = binder(ctx, server)

    override fun renderRecipe(screen: RecipeViewScreen, pos: ReliableClientRecipe.RecipePosition, guiGraphics: GuiGraphicsExtractor, mouseX: Int, mouseY: Int, partialTicks: Float) {
        renderer(guiGraphics, server)
    }
}