package co.stellarskys.stella.api.rrv.recipes

import cc.cassian.rrv.api.recipe.ReliableServerRecipeType
import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.rrv.RrvCompat
import co.stellarskys.stella.api.rrv.core.RecipeBuilder
import co.stellarskys.stella.api.rrv.core.ServerRecipe
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.width
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object KatRecipes {
    val SERVER: ReliableServerRecipeType<ServerRecipe> = ReliableServerRecipeType.register(
        Identifier.fromNamespaceAndPath(Stella.NAMESPACE, "kat")
    ) { ServerRecipe(SERVER) }

    val BUILDER = RecipeBuilder("kat", "Kat (Pet Upgrades)", 140, 95) { ItemStack(Items.BONE) }
        .slots(8) { def ->
            val centerX = (140 - 18) / 2
            def.addItemSlot(0, centerX, 10)
            val rowStartX = (140 - (5 * 18)) / 2
            for (i in 0 until 5) def.addItemSlot(i + 1, rowStartX + (i * 18), 35)
            def.addItemSlot(6, centerX, 65) // With items
            def.addItemSlot(7, centerX, 40) // No items
        }
        .bind { ctx, recipe ->
            val inputPet = recipe.ingredients.firstOrNull()
            val items = recipe.ingredients.drop(1)
            val outputPet = recipe.results.firstOrNull()

            inputPet?.let { ctx.bindSlot(0, it) }

            if (items.isNotEmpty()) {
                val startIdx = 3 - (items.size / 2)
                items.forEachIndexed { i, slot -> if (i < 5) ctx.bindSlot(startIdx + i, slot) }
                outputPet?.let { ctx.bindSlot(6, it) }
            } else {
                outputPet?.let { ctx.bindSlot(7, it) }
            }
        }
        .render { gui, recipe ->
            val hasItems = recipe.ingredients.size > 1
            val centerX = 140 / 2
            val textYStart = if (hasItems) 84 else 65

            if (hasItems) {
                Render2D.drawString(gui, "+", centerX - ("+".width() / 2), 26, color = Color.GRAY, shadow = false)
                Render2D.drawString(gui, "↓", centerX - ("↓".width() / 2), 54, color = Color.GRAY, shadow = false)
            } else {
                Render2D.drawString(gui, "↓", centerX - ("↓".width() / 2), 28, color = Color.GRAY, shadow = false)
            }

            val time = recipe.metadata["time"] ?: 0
            val coins = recipe.metadata["coins"] ?: 0

            val timeText = "§8⌚ " + RrvCompat.formatTime(time)
            Render2D.drawString(gui, timeText, centerX - (timeText.width() / 2), textYStart, shadow = false)

            if (coins > 0) {
                val coinText = "§6" + RrvCompat.formatCoins(coins)
                Render2D.drawString(gui, coinText, centerX - (coinText.width() / 2), textYStart + 10, shadow = false)
            }
        }
        .resultRedirect { stack, contents -> RrvCompat.checkPets(stack, contents.firstOrNull()?.validContents?.firstOrNull()) }
        .ingredientRedirect { stack, contents -> RrvCompat.checkPets(stack, contents.firstOrNull()?.validContents?.firstOrNull()) }

    val CLIENT = BUILDER.buildClientType()
}