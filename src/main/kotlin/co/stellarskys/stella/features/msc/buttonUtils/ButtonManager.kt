package co.stellarskys.stella.features.msc.buttonUtils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.GameEvent
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.render.Render2D
import co.stellarskys.stella.utils.render.Render2D.drawNVG
import co.stellarskys.vexel.Vexel
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import dev.deftu.omnicore.api.client.render.OmniResolution
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.client.gui.screens.Screen
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import tech.thatgravyboat.skyblockapi.api.remote.RepoItemsAPI
import java.io.File
import kotlin.jvm.optionals.getOrNull

object ButtonManager {
    private val buttons = mutableListOf<StellaButton>()
    private val itemCache = mutableMapOf<String, ItemStack>()

    private val buttonFile: File get() = File("config/Stella/buttons.json")

    val width = OmniResolution.scaledWidth.toFloat()
    val height = OmniResolution.scaledHeight.toFloat()

    init {
        load()

        EventBus.on<GameEvent.Stop> {
            save()
        }
    }

    fun getAll(): List<StellaButton> = buttons

    fun getButtonAt(anchor: AnchorType, index: Int): StellaButton? {
        return buttons.find { it.anchor == anchor && it.index == index }
    }

    fun add(button: StellaButton) {
        buttons.removeIf { it.anchor == button.anchor && it.index == button.index } // replace if exists
        buttons += button
    }

    fun remove(anchor: AnchorType, index: Int) {
        buttons.removeIf { it.anchor == anchor && it.index == index }
    }

    fun clear() {
        buttons.clear()
    }

    fun renderAll(context: GuiGraphics, invX: Int = 0, invY: Int = 0, width: Float = this.width, height: Float = this.height) {
        buttons.forEach { renderButton(context,it, invX, invY); renderButtonBackgroud(context,it, invX, invY) }
    }

    private fun renderButton(context: GuiGraphics, button: StellaButton, invX: Int, invY: Int) {
        val stack = getItem(button.iconId)
        val (x, y) = resolveAnchorPosition(button.anchor, button.index, invX, invY)

        val offsetX = (20f - 16f) / 2f
        val offsetY = (20f - 16f) / 2f

        Render2D.renderItem(context, stack, x.toFloat() + offsetX, y.toFloat() + offsetY, 1f)
    }

    private fun renderButtonBackgroud(context: GuiGraphics, button: StellaButton, invX: Int, invY: Int){
        if(!button.background) return
        val (x, y) = resolveAnchorPosition(button.anchor, button.index, invX, invY)
        context.drawNVG {
            Vexel.renderer.hollowRect(
                x.toFloat(),
                y.toFloat(),
                20f,
                20f,
                1f,
                0xFFAAAAAA.toInt(),
                4f
            )
        }
    }

    fun handleMouseClicked(gui: Screen, mouseX: Int, mouseY: Int): Boolean {
        val invX = (gui.width - 176) / 2
        val invY = (gui.height - 166) / 2
        val slotSize = 20

        for (button in buttons) {
            val (x, y) = resolveAnchorPosition(button.anchor, button.index, invX, invY)

            if (mouseX in x..(x + slotSize) && mouseY in y..(y + slotSize)) {
                var command = button.command.trim()

                if (!command.startsWith("/")) {
                    command = "/$command"
                }

                ChatUtils.sendCommand(command)
                return true
            }
        }

        return false
    }

    fun resolveAnchorPosition(anchor: AnchorType, index: Int, invX: Int, invY: Int): Pair<Int, Int> {
        val screenWidth = OmniResolution.scaledWidth
        val screenHeight = OmniResolution.scaledHeight

        val spacing = 24
        val slotSize = 20

        return when (anchor) {
            // Screen corners
            AnchorType.SCREEN_TOP_LEFT ->
                10 + (index * spacing) to 5

            AnchorType.SCREEN_TOP_RIGHT ->
                screenWidth - slotSize - 10 - (index * spacing) to 5

            AnchorType.SCREEN_BOTTOM_LEFT ->
                10 + (index * spacing) to screenHeight - slotSize - 5

            AnchorType.SCREEN_BOTTOM_RIGHT ->
                screenWidth - slotSize - 10 - (index * spacing) to screenHeight - slotSize - 5

            // Screen edges
            AnchorType.SCREEN_TOP ->
                (screenWidth / 2 - (2 * spacing)) + (index * spacing) - 10 to 5

            AnchorType.SCREEN_BOTTOM ->
                (screenWidth / 2 - (2 * spacing)) + (index * spacing) - 10 to screenHeight - slotSize - 5

            AnchorType.SCREEN_LEFT ->
                5 to (screenHeight / 2 - (2 * spacing)) + (index * spacing)

            AnchorType.SCREEN_RIGHT ->
                screenWidth - slotSize - 5 to (screenHeight / 2 - (2 * spacing)) + (index * spacing)

            // Inventory frame
            AnchorType.INVENTORY_TOP ->
                invX + 6 + (index * spacing) to invY - slotSize - 5

            AnchorType.INVENTORY_BOTTOM ->
                invX + 6 + (index * spacing) to invY + 170

            AnchorType.INVENTORY_LEFT ->
                invX - slotSize - 5 to invY + 12 + (index * spacing)

            AnchorType.INVENTORY_RIGHT ->
                invX + 176 + 5 to invY + 12 + (index * spacing)

            // Player model corners (explicit anchors are cleaner)
            AnchorType.PLAYER_MODEL_TOP_LEFT ->
                invX + 25 to invY + 8

            AnchorType.PLAYER_MODEL_TOP_RIGHT ->
                invX + 58 to invY + 8

            AnchorType.PLAYER_MODEL_BOTTOM_LEFT ->
                invX + 25 to invY + 58

            AnchorType.PLAYER_MODEL_BOTTOM_RIGHT ->
                invX + 58 to invY + 58

        }
    }

    fun save() {
        try {
            val gson = GsonBuilder().setPrettyPrinting().create()

            val json = gson.toJson(buttons)
            buttonFile.writeText(json)
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to save buttons", e)
        }
    }

    fun load() {
        if (!buttonFile.exists()) return

        try {
            val gson = Gson()
            val type = object : TypeToken<List<StellaButton>>() {}.type
            val loaded = gson.fromJson<List<StellaButton>>(buttonFile.readText(), type)
            buttons.clear()
            buttons.addAll(loaded)
        } catch (e: Exception) {
            Stella.LOGGER.error("Failed to load buttons", e)
        }
    }

    fun getItem(id: String) = ResourceLocation.tryParse(if (":" in id) id.lowercase() else "minecraft:${id.lowercase()}")?.let {
        BuiltInRegistries.ITEM.getOptional(it).getOrNull()?.defaultInstance
    } ?: RepoItemsAPI.getItem(id.uppercase())
}