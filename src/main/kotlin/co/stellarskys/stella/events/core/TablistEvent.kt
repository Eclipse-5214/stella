package co.stellarskys.stella.events.core

import net.minecraft.text.Text
import xyz.meowing.knit.api.events.Event

sealed class TablistEvent {
    class Change(
        val old: List<List<String>>,
        val new: List<List<Text>>,
    ) : Event()
}