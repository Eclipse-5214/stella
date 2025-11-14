package co.stellarskys.stella.events.core

import net.minecraft.world.item.ItemStack
import xyz.meowing.knit.api.events.Event

sealed class PlayerEvent {
    class HotbarChange(
        val slot: Int,
        val item: ItemStack
    ) : Event()
}