package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event
import net.minecraft.network.chat.Component

sealed class ChatEvent {
    class Receive(val message: Component, val isActionBar: Boolean) : Event(cancelable = true)
}