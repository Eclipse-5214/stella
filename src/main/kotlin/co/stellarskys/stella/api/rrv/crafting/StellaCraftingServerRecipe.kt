package co.stellarskys.stella.api.rrv.crafting

import cc.cassian.rrv.api.TagUtil
import cc.cassian.rrv.api.recipe.ReliableServerRecipe
import cc.cassian.rrv.api.recipe.ReliableServerRecipeType
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.Identifier

class StellaCraftingServerRecipe(
    private var inputs: Array<SlotContent?> = arrayOfNulls(9),
    private var output: SlotContent? = null
) : ReliableServerRecipe {
    companion object {
        val TYPE: ReliableServerRecipeType<StellaCraftingServerRecipe> = ReliableServerRecipeType.register(Identifier.fromNamespaceAndPath("stella", "skyblock_crafting")) { StellaCraftingServerRecipe() }
    }

    override fun writeToTag(tag: CompoundTag) {
        for (i in 0 until 9) {
            val slot = inputs[i]
            if (slot != null && !slot.isEmpty) {
                val stack = slot.validContents.first()
                tag.put("in$i", TagUtil.encodeItemStackOnServer(stack))
            }
        }

        output?.let {
            if (!it.isEmpty) {
                tag.put("out", TagUtil.encodeItemStackOnServer(it.validContents.first()))
            }
        }
    }

    override fun loadFromTag(tag: CompoundTag) {
        inputs = arrayOfNulls(9)
        for (i in 0 until 9) {
            tag.getCompoundOrEmpty("in$i").let { slotTag ->
                if (!slotTag.isEmpty) inputs[i] = SlotContent.of(TagUtil.decodeItemStackOnClient(slotTag))
            }
        }

        tag.getCompoundOrEmpty("out").let { outTag ->
            if (!outTag.isEmpty) {
                output = SlotContent.of(TagUtil.decodeItemStackOnClient(outTag))
            }
        }
    }

    override fun getRecipeType() = TYPE
    fun getInputs(): Array<SlotContent?> = inputs
    fun getOutput(): SlotContent? = output
}