package co.stellarskys.stella.api.rrv.recipes

import cc.cassian.rrv.api.recipe.ReliableServerRecipeType
import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.rrv.core.RecipeBuilder
import co.stellarskys.stella.api.rrv.core.ServerRecipe
import co.stellarskys.stella.utils.render.Render2D
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.awt.Color

object CraftingRecipes {
    val SERVER: ReliableServerRecipeType<ServerRecipe> = ReliableServerRecipeType.register(
        Identifier.fromNamespaceAndPath(Stella.NAMESPACE, "crafting")
    ) { ServerRecipe(SERVER) }

    val BUILDER = RecipeBuilder("crafting", "SkyBlock Crafting", 140, 75) { ItemStack(Items.CRAFTING_TABLE) }
        .slots(10) { def ->
            for (row in 0 until 3) {
                for (col in 0 until 3) {
                    def.addItemSlot(row * 3 + col, col * 18, row * 18)
                }
            }
            def.addItemSlot(9, 94, 18)
        }
        .bind { ctx, recipe ->
            val inputs = recipe.ingredients
            val output = recipe.results.firstOrNull()

            inputs.forEachIndexed { i, slot ->
                slot.let { ctx.bindSlot(i, it) }
            }
            output?.let { ctx.bindSlot(9, it) }
        }
        .render { gui, _ ->
            Render2D.drawString(gui, "→" , 62, 22, color = Color.GRAY, shadow = false)
        }

    val CLIENT = BUILDER.buildClientType()
}