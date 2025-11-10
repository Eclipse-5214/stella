package co.stellarskys.stella.events.core

import net.minecraft.text.Text
import xyz.meowing.knit.api.events.Event

sealed class ChatEvent {
    class Receive(val message: Text, val isActionBar: Boolean) : Event()
}