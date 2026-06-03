package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event
import co.stellarskys.stella.api.handlers.Signal
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

sealed class ChatEvent {
    class Receive(val message: Component, val isActionBar: Boolean) : Event(cancelable = true) {
        val stripped get() = message.stripped
        val string: String get() = message.string

        fun modify(new: String) = modify(Component.literal(new))
        fun modify(new: Component) { cancel(); Signal.fakeMessage(new) }

        infix fun matches(regex: Regex) = Match(this, regex.find(stripped))
        infix fun matchesRaw(regex: Regex) = Match(this, regex.find(string))

        class Match(val event: Receive, val match: MatchResult?) {
            infix fun run(block: (MatchResult) -> Unit) { match?.let { block(it) } }
            infix fun modifyC(new: (MatchResult) -> Component) { match?.let { event.modify(new(it)) } }
            infix fun modify(new: (MatchResult) -> String) { match?.let { event.modify(new(it)) } }
        }
    }
}