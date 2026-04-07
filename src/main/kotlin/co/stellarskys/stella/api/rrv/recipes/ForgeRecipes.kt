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

object ForgeRecipes {
    val SERVER: ReliableServerRecipeType<ServerRecipe> = ReliableServerRecipeType.register(
        Identifier.fromNamespaceAndPath(Stella.NAMESPACE, "forge")
    ) { ServerRecipe(SERVER) }

    val BUILDER = RecipeBuilder("forge", "The Forge", 140, 75) { ItemStack(Items.ANVIL) }
        .slots(8) { def ->
            val startX = (140 - (7 * 18)) / 2
            for (i in 0 .. 6) {
                def.addItemSlot(i, startX + (i * 18), 10)
            }
            def.addItemSlot(7, (140 - 18) / 2, 35)
        }
        .bind { ctx, recipe ->
            val inputs = recipe.ingredients
            val startSlot = (7 - inputs.size) / 2

            inputs.forEachIndexed { i, slot ->
                if (i < 7) ctx.bindSlot(startSlot + i, slot)
            }

            recipe.results.firstOrNull()?.let { ctx.bindSlot(7, it) }
        }
        .render { gui, recipe ->
            val centerX = 140 / 2
            Render2D.drawString(gui, "↓", centerX - ("↓".width() / 2), 29, color = Color.GRAY, shadow = false)

            val time = recipe.metadata["time"] ?: 0
            val coins = recipe.metadata["coins"] ?: 0

            if (time > 0) {
                val timeText = "§8⌚ " + RrvCompat.formatTime(time)
                Render2D.drawString(gui, timeText, centerX - (timeText.width() / 2), 62, shadow = false)
            }

            if (coins > 0) {
                val coinText = RrvCompat.formatCoins(coins)
                val yOffset = if (time > 0) 72 else 62
                Render2D.drawString(gui, coinText, centerX - (coinText.width() / 2), yOffset, shadow = false)
            }
        }

    val CLIENT = BUILDER.buildClientType()
}