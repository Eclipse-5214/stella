package co.stellarskys.stella.events.core

import net.minecraft.world.item.ItemStack
import co.stellarskys.stella.events.api.Event

sealed class PlayerEvent {
    class HotbarChange(
        val slot: Int,
        val item: ItemStack
    ) : Event()
}