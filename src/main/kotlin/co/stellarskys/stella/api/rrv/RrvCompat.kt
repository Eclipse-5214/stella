package co.stellarskys.stella.api.rrv

import cc.cassian.rrv.api.recipe.ItemView
import cc.cassian.rrv.common.recipe.ServerRecipeManager
import cc.cassian.rrv.common.recipe.cache.LowEndRecipeCache
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.api.rrv.crafting.StellaCraftingServerRecipe
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.utils.config
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.repolib.api.RepoAPI
import tech.thatgravyboat.repolib.api.recipes.CraftingRecipe
import tech.thatgravyboat.repolib.api.recipes.Recipe
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.id
import kotlin.collections.forEach

object RrvCompat {
    private var injected = false
    private var cachedRecipeEntries: List<ServerRecipeManager.ServerRecipeEntry>? = null
    val cachedStacks = mutableListOf<ItemStack>()

    val modInstalled = FabricLoader.getInstance().isModLoaded("rrv")
    val configEnabled by config.property<Boolean>("rrv")
    val enabled get() = configEnabled && RepoAPI.isInitialized() && modInstalled
    val width by config.property<Int>("rrv.width")

    fun checkIngredient(stack: ItemStack?, slots: List<SlotContent>): Boolean {
        val id = stack?.getSkyBlockId() ?: return false
        return slots.any { it.validContents.any { i -> i.getSkyBlockId() == id }}
    }

    fun sync() {
        if (!RepoAPI.isInitialized() || injected || !enabled) return

        cachedRecipeEntries = cachedRecipeEntries ?: RepoAPI.recipes().getRecipes(Recipe.Type.CRAFTING)
            .filterIsInstance<CraftingRecipe>()
            .map { recipe ->
                val id = Identifier.fromNamespaceAndPath("stella", "recipe_${recipe.result().id()?.lowercase() ?: "unknown"}")
                ServerRecipeManager.ServerRecipeEntry(id, convertToStellaRecipe(recipe))
            }

        RepoAPI.items().items().keys
            .mapNotNull { RepoItemsAPI.getItem(it).takeIf { s -> !s.isEmpty } }
            .also { cachedStacks.addAll(it) }

        client.execute {
            val cache = LowEndRecipeCache.INSTANCE
            val recipes = cachedRecipeEntries ?: return@execute
            if (cachedStacks.isEmpty()) return@execute

            cache.apply {
                stackSensitiveStartRecieved(cachedStacks.size)
                cachedStacks.forEach { stackSensitiveRecieved(ItemView.StackSensitive(it)) }
                stackSensitiveEndRecieved()
                cacheStartRecieved(1)
                startCaching(StellaCraftingServerRecipe.TYPE, recipes.size)
                recipes.forEach { cacheModRecipe(it) }
                endCaching(StellaCraftingServerRecipe.TYPE)
                processRecipes()
            }
            injected = true
        }
    }

    private fun convertToStellaRecipe(repo: CraftingRecipe): StellaCraftingServerRecipe {
        val inputs = Array(9) { i ->
            repo.inputs().getOrNull(i)?.id()?.let { RepoItemsAPI.getItemOrNull(it) }?.takeIf { !it.isEmpty }?.let { SlotContent.of(it) }
        }
        val output = RepoItemsAPI.getItem(repo.result().id() ?: "").let { SlotContent.of(it) }
        return StellaCraftingServerRecipe(inputs, output)
    }
}