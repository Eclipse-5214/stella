package co.stellarskys.stella.api.rrv

import cc.cassian.rrv.api.recipe.ItemView
import cc.cassian.rrv.api.recipe.ReliableServerRecipeType
import cc.cassian.rrv.common.recipe.ServerRecipeManager
import cc.cassian.rrv.common.recipe.cache.LowEndRecipeCache
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.api.rrv.crafting.StellaCraftingServerRecipe
import co.stellarskys.stella.api.rrv.forge.StellaForgeServerRecipe
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.utils.config
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.repolib.api.RepoAPI
import tech.thatgravyboat.repolib.api.recipes.CraftingRecipe
import tech.thatgravyboat.repolib.api.recipes.ForgeRecipe
import tech.thatgravyboat.repolib.api.recipes.Recipe
import tech.thatgravyboat.repolib.api.recipes.ingredient.*
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import tech.thatgravyboat.skyblockapi.api.remote.RepoPetsAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.id
import kotlin.collections.forEach

object RrvCompat {
    private var injected = false
    private val cachedEntries = mutableMapOf<ReliableServerRecipeType<*>, List<ServerRecipeManager.ServerRecipeEntry>>()
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

        val craftingRecipes = RepoAPI.recipes().getRecipes(Recipe.Type.CRAFTING)
            .filterIsInstance<CraftingRecipe>()
            .map { recipe ->
                val id = Identifier.fromNamespaceAndPath("stella", "crafting_${recipe.result().id()?.lowercase() ?: "unknown"}")
                ServerRecipeManager.ServerRecipeEntry(id, convertToStellaCraftingRecipe(recipe))
            }
        cachedEntries[StellaCraftingServerRecipe.TYPE] = craftingRecipes

        val forgeRecipes = RepoAPI.recipes().getRecipes(Recipe.Type.FORGE)
            .filterIsInstance<ForgeRecipe>()
            .map { recipe ->
                val id = Identifier.fromNamespaceAndPath("stella", "forge_${recipe.result().id()?.lowercase() ?: "unknown"}")
                ServerRecipeManager.ServerRecipeEntry(id, convertToStellaForgeRecipe(recipe))
            }
        cachedEntries[StellaForgeServerRecipe.TYPE] = forgeRecipes

        RepoAPI.items().items().keys
            .mapNotNull { RepoItemsAPI.getItem(it).takeIf { s -> !s.isEmpty } }
            .also { cachedStacks.addAll(it) }

        RepoAPI.pets().pets().forEach { (id, data) ->
            data.tiers().keys.forEach { rarityName ->
                SkyBlockRarity.fromNameOrNull(rarityName)?.let { rarity ->
                    cachedStacks.add(RepoPetsAPI.getPetAsItem(id, rarity))
                }
            }
        }

        client.execute {
            val cache = LowEndRecipeCache.INSTANCE
            if (cachedStacks.isEmpty()) return@execute
            if (cachedEntries.isEmpty()) return@execute

            cache.apply {
                stackSensitiveStartRecieved(cachedStacks.size)
                cachedStacks.forEach { stackSensitiveRecieved(ItemView.StackSensitive(it)) }
                stackSensitiveEndRecieved()
                cacheStartRecieved(2)

                val cRecipes = cachedEntries[StellaCraftingServerRecipe.TYPE] ?: emptyList()
                startCaching(StellaCraftingServerRecipe.TYPE, cRecipes.size)
                cRecipes.forEach { cacheModRecipe(it) }
                endCaching(StellaCraftingServerRecipe.TYPE)

                val fRecipes = cachedEntries[StellaForgeServerRecipe.TYPE] ?: emptyList()
                startCaching(StellaForgeServerRecipe.TYPE, fRecipes.size)
                fRecipes.forEach { cacheModRecipe(it) }
                endCaching(StellaForgeServerRecipe.TYPE)

                processRecipes()
            }
            injected = true
        }
    }

    fun resolveToIngredient(ingredient: CraftingIngredient): SlotContent? {
        if (ingredient is EmptyIngredient) return null

        val stack = when (ingredient) {
            is ItemIngredient -> RepoItemsAPI.getItem(ingredient.id)
            is PetIngredient -> {
                val rarity = SkyBlockRarity.entries.find { it.name.equals(ingredient.tier, true) } ?: SkyBlockRarity.COMMON
                RepoPetsAPI.getPetAsItem(ingredient.id, rarity)
            }
            else -> ItemStack.EMPTY
        }

        if (stack.isEmpty) return null
        stack.count = ingredient.count().coerceAtLeast(1)
        return SlotContent.of(stack)
    }

    private fun convertToStellaCraftingRecipe(repo: CraftingRecipe): StellaCraftingServerRecipe {
        val inputs = Array(9) { i -> repo.inputs().getOrNull(i)?.let { resolveToIngredient(it) } }
        val output = resolveToIngredient(repo.result())
        return StellaCraftingServerRecipe(inputs, output)
    }

    private fun convertToStellaForgeRecipe(repo: ForgeRecipe): StellaForgeServerRecipe {
        val inputs = repo.inputs().mapNotNull { resolveToIngredient(it) }
        val output = resolveToIngredient(repo.result())
        return StellaForgeServerRecipe(inputs = inputs, output = output, coins = repo.coins(), time = repo.time())
    }
}