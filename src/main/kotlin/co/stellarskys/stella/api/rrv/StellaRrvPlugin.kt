package co.stellarskys.stella.api.rrv

import cc.cassian.rrv.api.ReliableRecipeViewerClientPlugin
import cc.cassian.rrv.api.recipe.ItemView
import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.rrv.crafting.StellaCraftingClientRecipe
import co.stellarskys.stella.api.rrv.crafting.StellaCraftingServerRecipe
import kotlinx.coroutines.launch
import tech.thatgravyboat.repolib.api.RepoAPI

class StellaRrvPlugin: ReliableRecipeViewerClientPlugin {
    override fun onIntegrationInitialize() {
        ItemView.addClientRecipeWrapper(StellaCraftingServerRecipe.TYPE) { recipe ->
            listOf(
                StellaCraftingClientRecipe(
                    recipe.getInputs(),
                    recipe.getOutput(),
                    emptyArray()
                )
            )
        }

        ItemView.addClientReloadCallback {
            if (!RepoAPI.isInitialized()) return@addClientReloadCallback
            SyncManager.sync()
        }
    }
}