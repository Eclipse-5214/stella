package co.stellarskys.stella.api.rrv.forge

import cc.cassian.rrv.api.recipe.ReliableClientRecipe
import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.api.rrv.RrvCompat
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack
import java.awt.Color

class StellaForgeClientRecipe(private val serverRecipe: StellaForgeServerRecipe) : ReliableClientRecipe {
    override fun getViewType(): ReliableClientRecipeType = StellaForgeRecipeType
    override fun getIngredients(): List<SlotContent> = serverRecipe.getInputs().filterNotNull()
    override fun getResults(): List<SlotContent> = serverRecipe.getOutput()?.let { listOf(it) } ?: emptyList()
    override fun redirectsAsResult(stack: ItemStack?) = RrvCompat.checkIngredient(stack, results)
    override fun redirectsAsIngredient(stack: ItemStack?) = RrvCompat.checkIngredient(stack, ingredients)

    override fun bindSlots(ctx: RecipeViewMenu.SlotFillContext) {
        val inputs = serverRecipe.getInputs().filterNotNull()
        val startSlot = (7 - inputs.size) / 2

        inputs.forEachIndexed { i, slot ->
            if (i < 7) ctx.bindSlot(startSlot + i, slot)
        }

        serverRecipe.getOutput()?.let { ctx.bindSlot(8, it) }
    }

    override fun renderRecipe(
        screen: RecipeViewScreen,
        pos: ReliableClientRecipe.RecipePosition,
        guiGraphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float
    ) {
        val centerX = 140 / 2
        Render2D.drawString(guiGraphics, "↓", centerX - ("↓".width() / 2), 29, color = Color.GRAY, shadow = false)

        val timeSecs = serverRecipe.getTime()
        if (timeSecs > 0) {
            val timeText = "§8⌚" + if (timeSecs >= 3600) "${timeSecs / 3600}h" else if (timeSecs >= 60) "${timeSecs / 60}m" else "${timeSecs}s"
            Render2D.drawString(guiGraphics, timeText, centerX - (timeText.width() / 2), 62, shadow = false)
        }

        if (serverRecipe.getCoins() > 0) {
            val coinText = formatCoins(serverRecipe.getCoins())
            val yOffset = if (timeSecs > 0) 72 else 62
            Render2D.drawString(guiGraphics, coinText, centerX - (coinText.width() / 2), yOffset, shadow = false)
        }
    }

    private fun formatCoins(coins: Int): String {
        val formatted = when {
            coins >= 1_000_000 -> "${coins / 1_000_000}M"
            coins >= 1_000 -> "${coins / 1_000}k"
            else -> coins.toString()
        }
        return "§6$formatted"
    }
}