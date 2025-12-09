package co.stellarskys.stella.events.core

import net.minecraft.network.chat.Component
import co.stellarskys.stella.events.api.Event

sealed class TablistEvent {
    class Change(
        val old: List<List<String>>,
        val new: List<List<Component>>,
    ) : Event()
}