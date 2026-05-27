package co.stellarskys.stella.features.msc.profileUtils

import co.stellarskys.stella.api.handlers.Quasar
import co.stellarskys.stella.api.hypixel.SkyblockResponse
import com.google.gson.JsonObject
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.repo.apis.SkyBlockItemsRepo

object CollectionUtils {
    private const val BASE_URL = "https://api.hypixel.net/v2/resources/skyblock/collections"

    @Volatile
    private var collectionRegistry: Map<String, CollectionInfo> = emptyMap()

    data class CollectionInfo(val name: String, val category: String, val tiers: List<TierInfo>)
    data class TierInfo(val tier: Int, val amountRequired: Long)
    data class ItemProgress(val id: String, val name: String, val amount: Long, val currentTier: Int, val maxTier: Int, val nextTierAmount: Long)

    enum class CollectionType(val apiName: String, val displayName: String, val icon: () -> ItemStack) {
        FARMING("FARMING", "Farming", { Items.HAY_BLOCK.defaultInstance }),
        MINING("MINING", "Mining", { Items.DIAMOND_PICKAXE.defaultInstance }),
        COMBAT("COMBAT", "Combat", { Items.ROTTEN_FLESH.defaultInstance }),
        FORAGING("FORAGING", "Foraging", { Items.OAK_LOG.defaultInstance }),
        FISHING("FISHING", "Fishing", { Items.FISHING_ROD.defaultInstance }),
        RIFT("RIFT", "Rift", { Items.ENDER_EYE.defaultInstance });
    }

    fun load() {
        Quasar.fetch<JsonObject>(BASE_URL) { result ->
            val json = result.getOrNull()?.getAsJsonObject("collections") ?: return@fetch
            try {
                collectionRegistry = json.entrySet().flatMap { (category, data) ->
                    data.asJsonObject.getAsJsonObject("items").entrySet().map { (itemId, itemData) ->
                        val obj = itemData.asJsonObject
                        val tiers = obj.getAsJsonArray("tiers").map {
                            val t = it.asJsonObject
                            TierInfo(t["tier"].asInt, t["amountRequired"].asLong)
                        }
                        itemId to CollectionInfo(obj["name"].asString, category, tiers)
                    }
                }.toMap()
            } catch (ignored: Exception) {}
        }
    }

    fun isLoaded() = collectionRegistry.isNotEmpty()

    fun getCategoryProgress(category: CollectionType, member: SkyblockResponse.SkyblockMember): List<ItemProgress> {
        val playerColl = member.collection
        return collectionRegistry.filter { it.value.category == category.apiName }.map { (id, info) ->
            val amount = playerColl[id]?.toLong() ?: 0L
            val currentTier = info.tiers.lastOrNull { it.amountRequired <= amount }?.tier ?: 0
            val nextTier = info.tiers.find { it.tier == currentTier + 1 }
            
            ItemProgress(id, info.name, amount, currentTier, info.tiers.lastOrNull()?.tier ?: 0, nextTier?.amountRequired ?: 0L)
        }.sortedWith(compareByDescending<ItemProgress> { it.amount > 0 }.thenByDescending { it.amount })
    }

    fun getIcon(id: String): ItemStack = SkyBlockItemsRepo.getItemStack(id) ?: Items.PAPER.defaultInstance
}
