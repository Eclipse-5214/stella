package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event
import net.minecraft.network.chat.Component
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped

abstract class ChatEvent(val message: Component): Event(cancelable = true) {
    val stripped get() = message.stripped
    val string: String get() = message.string

    open infix fun matches(regex: Regex) = Match(regex.find(stripped))
    open infix fun matchesRaw(regex: Regex) = Match(regex.find(string))

    class Receive(message: Component): ChatEvent(message)
    class ActionBar(message: Component): ChatEvent(message)

    abstract class Modify(message: Component): ChatEvent(message) {
        private var result: Component = message
        fun getResult(): Component = if(cancelled) Component.empty() else result

        fun modify(new: String) { result = Component.literal(new) }
        fun modify(new: Component) { result = new }

        override infix fun matches(regex: Regex) = MMatch(this, regex.find(stripped))
        override infix fun matchesRaw(regex: Regex) = MMatch(this, regex.find(string))

        class MMatch(val event: Modify, match: MatchResult?): Match(match) {
            infix fun modifyC(new: (MatchResult) -> Component) { match?.let { event.modify(new(it)) } }
            infix fun modify(new: (MatchResult) -> String) { match?.let { event.modify(new(it)) } }
        }

        class Receive(message: Component): Modify(message)
        class ActionBar(message: Component): Modify(message)
    }


    abstract class Channel(message: Component, val name: String, val contents: String): ChatEvent(message) {
        override infix fun matches(regex: Regex) = Match(regex.find(contents))

        class Party(message: Component, name: String, contents: String): Channel(message, name, contents)
        class Guild(message: Component, name: String, contents: String): Channel(message, name, contents)
        class Coop(message: Component, name: String, contents: String): Channel(message, name, contents)
        class Private(message: Component, name: String, contents: String, val incoming: Boolean): Channel(message, name, contents)

        companion object {
            private val pattern = """^(?<t>Party >|Co-op >|Guild >|From|To) (?:\[[^]]+] )?(?<n>\w{1,16}): (?<m>.+)$""".toRegex()

            fun match(message: Component): Channel? {
                val match = pattern.find(message.stripped) ?: return null
                val name = match.groups["n"]?.value ?: ""
                val contents = match.groups["m"]?.value ?: ""
                return when (match.groups["t"]!!.value) {
                    "Party >" -> Party(message, name, contents)
                    "Co-op >" -> Coop(message, name, contents)
                    "Guild >" -> Guild(message, name, contents)
                    "From"    -> Private(message, name, contents, true)
                    else      -> Private(message, name, contents, false)
                }
            }
        }
    }

    open class Match(val match: MatchResult?) {
        infix fun run(block: (MatchResult) -> Unit) { match?.let { block(it) } }
    }
}