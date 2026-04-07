package co.stellarskys.stella.api.rrv.crafting

import cc.cassian.rrv.api.recipe.ReliableClientRecipe
import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import cc.cassian.rrv.common.recipe.inventory.RecipeViewScreen
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.api.zenith.client
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.components.Button
import net.minecraft.network.chat.Component

class StellaCraftingClientRecipe(
    private val inputs: Array<SlotContent?>,
    private val output: SlotContent?,
    private val wikiUrls: Array<String>
) : ReliableClientRecipe {

    private var wikiButton: Button? = null

    override fun getViewType(): ReliableClientRecipeType = StellaCraftingRecipeType

    override fun bindSlots(ctx: RecipeViewMenu.SlotFillContext) {
        // Bind inputs 0-8
        inputs.forEachIndexed { i, slot ->
            slot?.let { ctx.bindSlot(i, it) }
        }
        // Bind output 9
        output?.let { ctx.bindSlot(9, it) }
    }

    override fun getIngredients(): List<SlotContent> = inputs.filterNotNull()

    override fun getResults(): List<SlotContent> = output?.let { listOf(it) } ?: emptyList()

    override fun renderRecipe(
        screen: RecipeViewScreen,
        pos: ReliableClientRecipe.RecipePosition,
        guiGraphics: GuiGraphicsExtractor,
        mouseX: Int,
        mouseY: Int,
        partialTicks: Float
    ) {
        // Draw the Crafting Arrow (ASCII arrow or literal)
        guiGraphics.text(
            client.font,
            Component.literal("→"),
            62, 22,
            0xFF404040.toInt(),
            false
        )

        /*
        // Wiki Button Logic
        // Note: You will need a 'SkyblockRecipeUtil' equivalent or
        // implement the button addition directly here.
        if (wikiButton == null || !screen.children().contains(wikiButton)) {
            // wikiButton = YourUtil.addWikiButton(screen, wikiUrls, pos.left(), pos.top() + 56)
        }
         */
    }
}