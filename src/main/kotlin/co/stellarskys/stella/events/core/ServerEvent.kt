package co.stellarskys.stella.events.core

import co.stellarskys.stella.events.api.Event

sealed class ServerEvent {
    class Connect : Event()

    class Disconnect : Event()
}