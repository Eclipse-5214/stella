package co.stellarskys.stella.api.rrv

import cc.cassian.rrv.api.recipe.ItemView
import cc.cassian.rrv.common.recipe.ServerRecipeManager
import cc.cassian.rrv.common.recipe.cache.LowEndRecipeCache
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Chronos.millis
import co.stellarskys.stella.api.rrv.crafting.StellaCraftingServerRecipe
import co.stellarskys.stella.api.zenith.client
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.repolib.api.RepoAPI
import tech.thatgravyboat.repolib.api.RepoStatus
import tech.thatgravyboat.repolib.api.RepoVersion
import tech.thatgravyboat.repolib.api.recipes.CraftingRecipe
import tech.thatgravyboat.repolib.api.recipes.Recipe
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import tech.thatgravyboat.skyblockapi.api.remote.id

object SyncManager {
    private var injected = false // This is the key
    private var cachedStacks: List<ItemStack>? = null
    private var cachedRecipeEntries: List<ServerRecipeManager.ServerRecipeEntry>? = null

    // Call this if the Repo actually updates/reloads to force a re-inject
    fun invalidate() {
        injected = false
        cachedStacks = null
        cachedRecipeEntries = null
    }

    fun sync() {
        var start = Chronos.now

        if (!RepoAPI.isInitialized()) return

        // If we already spoofed the cache, DO NOTHING.d
        // RRV does not actually clear the LowEndRecipeCache on lobby swaps
        // unless the player manually reloads tags/resources.
        if (injected) return

        // 1. Prepare Data (Only if not cached)
        if (cachedRecipeEntries == null) {
            val repoRecipes = RepoAPI.recipes().getRecipes(Recipe.Type.CRAFTING)
            cachedRecipeEntries = repoRecipes.filterIsInstance<CraftingRecipe>().map { recipe ->
                val resultId = recipe.result().id()?.lowercase() ?: "unknown"
                val id = Identifier.fromNamespaceAndPath("stella", "recipe_$resultId")
                ServerRecipeManager.ServerRecipeEntry(id, convertToStellaRecipe(recipe))
            }

            println("Recipe Mapping took: ${start.since.millis}ms")
        }

        start = Chronos.now
        if (cachedStacks == null) {
            cachedStacks = RepoAPI.items().items().keys.map { id ->
                RepoItemsAPI.getItem(id)
            }.filter { !it.isEmpty }
            println("Item Stack building took: ${start.since.millis}ms")
        }

        // 2. Inject (Only runs once)
        start = Chronos.now
        client.execute {
            // Re-check injected inside the scheduled task to be thread-safe
            if (injected) return@execute

            val cache = LowEndRecipeCache.INSTANCE
            val stacks = cachedStacks ?: return@execute
            val recipes = cachedRecipeEntries ?: return@execute

            cache.stackSensitiveStartRecieved(stacks.size)
            stacks.forEach { cache.stackSensitiveRecieved(ItemView.StackSensitive(it)) }
            cache.stackSensitiveEndRecieved()

            cache.cacheStartRecieved(1)
            cache.startCaching(StellaCraftingServerRecipe.TYPE, recipes.size)
            recipes.forEach { cache.cacheModRecipe(it) }
            cache.endCaching(StellaCraftingServerRecipe.TYPE)

            cache.processRecipes()
            println("RRV Cache Processing took: ${start.since.millis}ms")

            injected = true // Stop the lag on future calls
        }
    }

    private fun convertToStellaRecipe(repo: CraftingRecipe): StellaCraftingServerRecipe {
        val inputs = arrayOfNulls<SlotContent>(9)
        val repoInputs = repo.inputs()

        for (i in 0 until 9) {
            if (i < repoInputs.size) {
                val ingredientId = repoInputs[i].id() ?: ""
                val stack = RepoItemsAPI.getItemOrNull(ingredientId)
                if (stack?.isEmpty == false) {
                    inputs[i] = SlotContent.of(stack)
                }
            }
        }

        val outputStack = RepoItemsAPI.getItem(repo.result().id() ?: "")
        return StellaCraftingServerRecipe(inputs, SlotContent.of(outputStack))
    }
}