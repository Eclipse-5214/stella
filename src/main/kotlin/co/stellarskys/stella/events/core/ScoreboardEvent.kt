package co.stellarskys.stella.events.core

import net.minecraft.text.Text
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import xyz.meowing.knit.api.events.Event

sealed class ScoreboardEvent {
    class UpdateTitle(
        val old: String?,
        val new: String
    ) : Event()

    class Update(
        val old: List<String>,
        val new: List<String>,
        val components: List<Text>,
    ) : Event() {
        val added: List<String> = new - old.toSet()
        val removed: List<String> = old - new.toSet()

        private val addedSet: Set<String> = added.toSet()
        private val removedSet: Set<String> = removed.toSet()

        val addedComponents: List<Text> = components.filter { it.stripped in addedSet }
        val removedComponents: List<Text> = components.filter { it.stripped in removedSet }
    }
}