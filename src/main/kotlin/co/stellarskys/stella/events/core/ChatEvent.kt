package co.stellarskys.stella.events.core

import net.minecraft.network.chat.Component
import xyz.meowing.knit.api.events.Event

sealed class ChatEvent {
    class Receive(val message: Component, val isActionBar: Boolean) : Event()
}