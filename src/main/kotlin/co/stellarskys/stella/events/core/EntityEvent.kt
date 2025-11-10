package co.stellarskys.stella.events.core


import net.minecraft.entity.Entity
import xyz.meowing.knit.api.events.Event

sealed class EntityEvent {
    class Death(
        val entity: Entity
    ) : Event()
}