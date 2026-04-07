package co.stellarskys.stella.api.rrv

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin
import cc.cassian.rrv.api.recipe.ItemView
import co.stellarskys.stella.api.rrv.crafting.StellaCraftingClientRecipe
import co.stellarskys.stella.api.rrv.crafting.StellaCraftingServerRecipe
import co.stellarskys.stella.api.rrv.forge.StellaForgeClientRecipe
import co.stellarskys.stella.api.rrv.forge.StellaForgeServerRecipe
import tech.thatgravyboat.repolib.api.RepoAPI

class StellaRrvPlugin: ReliableRecipeViewerClientPlugin {
    override fun onIntegrationInitialize() {
        if (!RrvCompat.enabled) return

        ItemView.addClientRecipeWrapper(StellaCraftingServerRecipe.TYPE) { recipe ->
            listOf(StellaCraftingClientRecipe(recipe.getInputs(), recipe.getOutput()))
        }

        ItemView.addClientRecipeWrapper(StellaForgeServerRecipe.TYPE) { recipe ->
            listOf(StellaForgeClientRecipe(recipe))
        }

        ItemView.addClientReloadCallback {
            if (!RepoAPI.isInitialized()) return@addClientReloadCallback
            RrvCompat.sync()
        }
    }
}