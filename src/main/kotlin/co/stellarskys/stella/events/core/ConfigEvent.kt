package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event

sealed class ConfigEvent {
    class Update(
        val id: String,
        val old: Any?,
        val new: Any?
    ): Event()
}