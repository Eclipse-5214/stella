package co.stellarskys.stella.api.rrv.core

import cc.cassian.rrv.api.TagUtil
import cc.cassian.rrv.api.recipe.ReliableServerRecipe
import cc.cassian.rrv.api.recipe.ReliableServerRecipeType
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import net.minecraft.nbt.CompoundTag

class ServerRecipe(
    private val recipeType: ReliableServerRecipeType<ServerRecipe>
) : ReliableServerRecipe {
    var ingredients = mutableListOf<SlotContent>()
    var results = mutableListOf<SlotContent>()
    val metadata = mutableMapOf<String, Int>()

    override fun getRecipeType() = recipeType

    override fun writeToTag(tag: CompoundTag) {
        metadata.forEach { (k, v) -> tag.putInt(k, v) }
        ingredients.forEachIndexed { i, s ->
            if (!s.isEmpty) tag.put("in$i", TagUtil.encodeItemStackOnServer(s.validContents.first()))
        }
        results.forEachIndexed { i, s ->
            if (!s.isEmpty) tag.put("out$i", TagUtil.encodeItemStackOnServer(s.validContents.first()))
        }
    }

    override fun loadFromTag(tag: CompoundTag) {
        metadata.keys.forEach { k -> if (tag.contains(k)) metadata[k] = tag.getInt(k).orElse(0) }

        ingredients.clear()
        var i = 0
        while (tag.contains("in$i")) {
            ingredients.add(SlotContent.of(TagUtil.decodeItemStackOnClient(tag.getCompound("in$i").orElse(null))))
            i++
        }

        results.clear()
        i = 0
        while (tag.contains("out$i")) {
            results.add(SlotContent.of(TagUtil.decodeItemStackOnClient(tag.getCompound("out$i").orElse(null))))
            i++
        }
    }
}


