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
        val TYPE: ReliableServerRecipeType<StellaCraftingServerRecipe> =
            ReliableServerRecipeType.register(
                Identifier.fromNamespaceAndPath("stella", "skyblock_crafting")
            ) { StellaCraftingServerRecipe() }
    }

    override fun getRecipeType() = TYPE

    override fun writeToTag(tag: CompoundTag) {
        // Encode each input slot individually
        for (i in 0 until 9) {
            val slot = inputs[i]
            if (slot != null && !slot.isEmpty) {
                // Get the first item in the slot and encode it losslessly
                val stack = slot.validContents.first()
                tag.put("in$i", TagUtil.encodeItemStackOnServer(stack))
            }
        }

        // Encode the output losslessly
        output?.let {
            if (!it.isEmpty) {
                tag.put("out", TagUtil.encodeItemStackOnServer(it.validContents.first()))
            }
        }
    }

    override fun loadFromTag(tag: CompoundTag) {
        inputs = arrayOfNulls(9)
        for (i in 0 until 9) {
            // Use ifPresent to handle the Optional returning from getCompoundOrEmpty
            tag.getCompoundOrEmpty("in$i")?.let { slotTag ->
                if (!slotTag.isEmpty) {
                    inputs[i] = SlotContent.of(TagUtil.decodeItemStackOnClient(slotTag))
                }
            }
        }

        tag.getCompoundOrEmpty("out")?.let { outTag ->
            if (!outTag.isEmpty) {
                output = SlotContent.of(TagUtil.decodeItemStackOnClient(outTag))
            }
        }
    }

    fun getInputs(): Array<SlotContent?> = inputs
    fun getOutput(): SlotContent? = output
}