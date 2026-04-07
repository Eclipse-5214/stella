package co.stellarskys.stella.api.rrv.core

import cc.cassian.rrv.api.recipe.ReliableClientRecipeType
import cc.cassian.rrv.common.recipe.inventory.RecipeViewMenu
import cc.cassian.rrv.common.recipe.inventory.SlotContent
import co.stellarskys.stella.Stella
import co.stellarskys.stella.api.rrv.RrvCompat
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.resources.Identifier
import net.minecraft.world.item.ItemStack

class RecipeBuilder(
    val rid: String,
    val disName: String,
    private val width: Int,
    private val height: Int,
    private val iconSupplier: () -> ItemStack
) {
    private var sc: Int = 0
    private var slotPlacer: (RecipeViewMenu.SlotDefinition) -> Unit = {}
    private var slotBinder: (RecipeViewMenu.SlotFillContext, ServerRecipe) -> Unit = { _, _ -> }
    private var recipeRenderer: (GuiGraphicsExtractor, ServerRecipe) -> Unit = { _, _ -> }
    private var resultRedirect: (ItemStack?, List<SlotContent>) -> Boolean = { stack, results -> RrvCompat.checkIngredient(stack, results) }
    private var ingredientRedirect: (ItemStack?, List<SlotContent>) -> Boolean = { stack, ingredients -> RrvCompat.checkIngredient(stack, ingredients) }

    fun slots(count: Int, placer: (RecipeViewMenu.SlotDefinition) -> Unit) = apply { this.sc = count; this.slotPlacer = placer }
    fun bind(binder: (RecipeViewMenu.SlotFillContext, ServerRecipe) -> Unit) = apply { this.slotBinder = binder }
    fun render(renderer: (GuiGraphicsExtractor, ServerRecipe) -> Unit) = apply { this.recipeRenderer = renderer }
    fun resultRedirect(logic: (ItemStack?, List<SlotContent>) -> Boolean) = apply { this.resultRedirect = logic }
    fun ingredientRedirect(logic: (ItemStack?, List<SlotContent>) -> Boolean) = apply { this.ingredientRedirect = logic }

    fun buildClientType(): ReliableClientRecipeType {
        return object : ReliableClientRecipeType {
            private val _icon by lazy { iconSupplier() }
            private val _name by lazy { Component.literal(disName) }

            override fun getId() = Identifier.fromNamespaceAndPath(Stella.NAMESPACE, rid)
            override fun getDisplayName() = _name
            override fun getDisplayWidth() = width
            override fun getDisplayHeight() = height
            override fun getSlotCount() = sc
            override fun getIcon() = _icon
            override fun getCraftReferences() = listOf(_icon)
            override fun getGuiTexture(): Identifier? = null
            override fun placeSlots(def: RecipeViewMenu.SlotDefinition) = slotPlacer(def)
        }
    }

    fun createClientRecipe(serverRecipe: ServerRecipe, type: ReliableClientRecipeType)
         = ClientRecipe(serverRecipe, type, slotBinder, recipeRenderer, resultRedirect, ingredientRedirect)
}