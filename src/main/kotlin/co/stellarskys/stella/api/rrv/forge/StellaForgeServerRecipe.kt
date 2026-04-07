package co.stellarskys.stella.api.rrv.forge

import cc.cassian.rrv.api.TagUtil
import cc.cassian.rrv.api.recipe.ReliableServerRecipe
import cc.cassian.rrv.api.recipe.ReliableServerRecipeType
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier

class StellaForgeServerRecipe(
    private var inputs: List<SlotContent?> = emptyList(),
    private var output: SlotContent? = null,
    private var coins: Int = 0,
    private var time: Int = 0
) : ReliableServerRecipe {
    companion object {
        val TYPE: ReliableServerRecipeType<StellaForgeServerRecipe> = ReliableServerRecipeType.register(
            Identifier.fromNamespaceAndPath("stella", "forge")
        ) { StellaForgeServerRecipe() }
    }

    override fun writeToTag(tag: CompoundTag) {
        tag.putInt("coins", coins)
        tag.putInt("time", time)
        inputs.forEachIndexed { i, slot ->
            slot?.let {
                if (!it.isEmpty) {
                    tag.put("in$i", TagUtil.encodeItemStackOnServer(it.validContents.first()))
                }
            }
        }
        output?.let {
            if (!it.isEmpty) {
                tag.put("out", TagUtil.encodeItemStackOnServer(it.validContents.first()))
            }
        }
    }

    override fun loadFromTag(tag: CompoundTag) {
        this.coins = tag.getInt("coins").orElse(0)
        this.time = tag.getInt("time").orElse(0)

        val inputList = mutableListOf<SlotContent?>()
        var i = 0
        while (tag.contains("in$i")) {
            tag.getCompound("in$i").ifPresent { compound ->
                inputList.add(SlotContent.of(TagUtil.decodeItemStackOnClient(compound)))
            }
            i++
        }
        this.inputs = inputList

        this.output = tag.getCompound("out").map { compound ->
            SlotContent.of(TagUtil.decodeItemStackOnClient(compound))
        }.orElse(null)
    }

    override fun getRecipeType() = TYPE
    fun getInputs() = inputs
    fun getOutput() = output
    fun getCoins() = coins
    fun getTime() = time
}