package co.stellarskys.stella.api.rrv

import co.stellarskys.stella.utils.config
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.Screen
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

object RrvCompat {
    val enabled by config.property<Boolean>("rrv")
    val width by config.property<Int>("rrv.width")

    private var lastHoveredStack: ItemStack? = null
    private var cachedTooltip: List<Component> = listOf()

    @JvmStatic
    fun getCachedTooltip(minecraft: Minecraft, stack: ItemStack): List<Component> {
        // '===' in Kotlin checks if it's the EXACT same memory address (Identity)
        // This is much faster than .equals() for ItemStacks
        if (stack === lastHoveredStack) {
            return cachedTooltip
        }

        // Cache Miss: Generate the tooltip once
        lastHoveredStack = stack
        // This call triggers the Skyblock-API lore/stats generation
        cachedTooltip = Screen.getTooltipFromItem(minecraft, stack)

        return cachedTooltip
    }

    // Call this when changing lobbies or reloading the repo
    @JvmStatic
    fun clearTooltipCache() {
        lastHoveredStack = null
        cachedTooltip = listOf()
    }
}