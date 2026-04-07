package co.stellarskys.stella.api.rrv

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin
import cc.cassian.rrv.api.recipe.ItemView
import co.stellarskys.stella.api.rrv.recipes.*
import tech.thatgravyboat.repolib.api.RepoAPI

class StellaRrvPlugin: ReliableRecipeViewerClientPlugin {
    override fun onIntegrationInitialize() {
        if (!RrvCompat.enabled) return

        ItemView.addClientRecipeWrapper(CraftingRecipes.SERVER) { recipe ->
            listOf(CraftingRecipes.BUILDER.createClientRecipe(recipe, CraftingRecipes.CLIENT))
        }

        ItemView.addClientRecipeWrapper(ForgeRecipes.SERVER) { recipe ->
            listOf(ForgeRecipes.BUILDER.createClientRecipe(recipe, ForgeRecipes.CLIENT))
        }

        ItemView.addClientRecipeWrapper(KatRecipes.SERVER) { recipe ->
            listOf(KatRecipes.BUILDER.createClientRecipe(recipe, KatRecipes.CLIENT))
        }

        ItemView.addClientReloadCallback {
            if (!RepoAPI.isInitialized()) return@addClientReloadCallback
            RrvCompat.sync()
        }
    }
}