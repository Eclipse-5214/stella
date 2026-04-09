package co.stellarskys.stella.api.rrv

import cc.cassian.rrv.api.recipe.ItemView
import cc.cassian.rrv.api.recipe.ReliableServerRecipeType
import cc.cassian.rrv.common.gui.RrvClientSettingsScreen
import cc.cassian.rrv.common.recipe.ServerRecipeManager
import cc.cassian.rrv.common.recipe.cache.LowEndRecipeCache
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.rrv.core.ServerRecipe
import co.stellarskys.stella.api.rrv.recipes.*
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.RepoEvent
import co.stellarskys.stella.utils.config
import net.fabricmc.loader.api.FabricLoader
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack
import tech.thatgravyboat.repolib.api.RepoAPI
import tech.thatgravyboat.repolib.api.recipes.*
import tech.thatgravyboat.repolib.api.recipes.ingredient.*
import tech.thatgravyboat.skyblockapi.api.data.SkyBlockRarity
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import tech.thatgravyboat.skyblockapi.api.remote.RepoPetsAPI
import tech.thatgravyboat.skyblockapi.api.remote.api.SkyBlockId.Companion.getSkyBlockId
import tech.thatgravyboat.skyblockapi.api.remote.id
import tech.thatgravyboat.skyblockapi.utils.extentions.getLore
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import kotlin.collections.forEach

@Module
object RrvCompat {
    private val PET_REGEX = """\[Lvl\s\d+]\s+([a-zA-Z0-9 ]+)""".toRegex()

    private val cachedEntries = mutableMapOf<ReliableServerRecipeType<*>, List<ServerRecipeManager.ServerRecipeEntry>>()
    val cachedStacks = mutableListOf<ItemStack>()

    val configEnabled by config.property<Boolean>("rrv")
    val width by config.property<Int>("rrv.width")
    val modInstalled get() = FabricLoader.getInstance().isModLoaded("rrv")
    val enabled get() = configEnabled && modInstalled

    var injected = false
        private set

    init { EventBus.on<RepoEvent.Success> { sync() } }

    fun checkIngredient(s: ItemStack?, slots: List<SlotContent>) = s?.getSkyBlockId()?.let { id -> slots.any { it.validContents.any { i -> i.getSkyBlockId() == id } } } ?: false

    fun checkPets(q: ItemStack?, s: ItemStack?): Boolean {
        val (qn, qr) = q?.getPetInfo() ?: return false
        val (sn, sr) = s?.getPetInfo() ?: return false
        return qn.equals(sn, true) && qr == sr
    }

    fun ItemStack.getPetInfo(): Pair<String, SkyBlockRarity> {
        val raw = displayName.stripped
        return (PET_REGEX.find(raw)?.groupValues?.get(1) ?: raw) to SkyBlockRarity.fromName(getLore().lastOrNull()?.stripped ?: "COMMON")
    }

    fun sync() {
        if (!RepoAPI.isInitialized() || injected || !enabled) return

        cachedEntries[CraftingRecipes.SERVER] = RepoAPI.recipes().getRecipes(Recipe.Type.CRAFTING).filterIsInstance<CraftingRecipe>().map {
            ServerRecipeManager.ServerRecipeEntry(id("crafting", it.result()), convertToCraftingRecipe(it))
        }
        cachedEntries[ForgeRecipes.SERVER] = RepoAPI.recipes().getRecipes(Recipe.Type.FORGE).filterIsInstance<ForgeRecipe>().map {
            ServerRecipeManager.ServerRecipeEntry(id("forge", it.result()), convertToForgeRecipe(it))
        }
        cachedEntries[KatRecipes.SERVER] = RepoAPI.recipes().getRecipes(Recipe.Type.KAT).filterIsInstance<KatRecipe>().map {
            val sfx = (it.output() as? PetIngredient)?.tier?.lowercase() ?: "unknown"
            ServerRecipeManager.ServerRecipeEntry(id("kat", it.output()).withPath { p -> "${p}_$sfx" }, convertToKatRecipe(it))
        }

        cachedStacks.apply {
            clear()
            addAll(RepoAPI.items().items().keys.mapNotNull { RepoItemsAPI.getItem(it).takeIf { s -> !s.isEmpty } })
            addAll(RepoAPI.pets().pets().flatMap { (id, d) -> d.tiers().keys.mapNotNull { r -> SkyBlockRarity.fromNameOrNull(r)?.let { RepoPetsAPI.getPetAsItem(id, it) } } })
        }

        client.execute {
            if (cachedStacks.isEmpty() || cachedEntries.isEmpty()) return@execute
            LowEndRecipeCache.INSTANCE.apply {
                stackSensitiveStartRecieved(cachedStacks.size)
                cachedStacks.forEach { stackSensitiveRecieved(ItemView.StackSensitive(it)) }
                stackSensitiveEndRecieved()
                cacheStartRecieved(3)
                listOf(CraftingRecipes.SERVER, ForgeRecipes.SERVER, KatRecipes.SERVER).forEach { cacheType(it, cachedEntries) }
                processRecipes()
            }
            injected = true
        }
    }

    fun resolveToIngredient(i: CraftingIngredient?): SlotContent {
        if (i is EmptyIngredient || i == null) return SlotContent.of()
        val s = when (i) {
            is ItemIngredient -> RepoItemsAPI.getItem(i.id)
            is PetIngredient -> RepoPetsAPI.getPetAsItem(i.id, SkyBlockRarity.entries.find { it.name.equals(i.tier, true) } ?: SkyBlockRarity.COMMON)
            else -> ItemStack.EMPTY
        }.apply { count = i.count().coerceAtLeast(1) }
        return SlotContent.of(s)
    }

    private fun convertToCraftingRecipe(r: CraftingRecipe) = ServerRecipe(CraftingRecipes.SERVER).apply { (0..8).forEach { ingredients.add(resolveToIngredient(r.inputs().getOrNull(it))) }; addResult(r.result()) }
    private fun convertToForgeRecipe(r: ForgeRecipe) = ServerRecipe(ForgeRecipes.SERVER).apply { r.inputs().take(7).forEach { ingredients.add(resolveToIngredient(it)) }; addResult(r.result()) }
    private fun convertToKatRecipe(r: KatRecipe) = ServerRecipe(KatRecipes.SERVER).apply { addResult(r.output()); ingredients.add(resolveToIngredient(r.input())); r.items().forEach { ingredients.add(resolveToIngredient(it)) }; metadata["coins"] = r.coins(); metadata["time"] = r.time() }
    private fun id(type: String, item: CraftingIngredient) = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, "${type}_${item.id()?.lowercase() ?: "unknown"}")

    fun formatTime(s: Int) = when { s >= 86400 -> "${s/86400}d"; s >= 3600 -> "${s/3600}h"; s >= 60 -> "${s/60}m"; else -> "${s}s" }
    fun formatCoins(c: Int) = when { c >= 1_000_000 -> "${c/1_000_000}M"; c >= 1_000 -> "${c/1_000}k"; else -> c.toString() }
    fun openConfig() { Chronos.Tick post { client.setScreen(RrvClientSettingsScreen(client.screen)) } }

    private fun ServerRecipe.addResult(i: CraftingIngredient?) = results.add(resolveToIngredient(i))
    private fun LowEndRecipeCache.cacheType(t: ReliableServerRecipeType<*>, e: Map<ReliableServerRecipeType<*>, List<ServerRecipeManager.ServerRecipeEntry>>) = e[t]?.let { r -> startCaching(t, r.size); r.forEach { cacheModRecipe(it) }; endCaching(t) }
}