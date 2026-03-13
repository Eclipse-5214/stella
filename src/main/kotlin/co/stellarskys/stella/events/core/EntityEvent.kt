package co.stellarskys.stella.events.core


import net.minecraft.world.entity.Entity
import co.stellarskys.stella.api.events.Event

sealed class EntityEvent {
    class Death(
        val entity: Entity
    ) : Event()
}