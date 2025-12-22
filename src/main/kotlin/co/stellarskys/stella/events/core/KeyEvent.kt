package co.stellarskys.stella.events.core

import co.stellarskys.stella.events.api.Event

sealed class KeyEvent {
    class Press(
        val keyCode: Int,
        val scanCode: Int,
        val modifiers: Int
    ) : Event(cancelable = true)

    class Release(
        val keyCode: Int,
        val scanCode: Int,
        val modifiers: Int
    ) : Event(cancelable = true)
}