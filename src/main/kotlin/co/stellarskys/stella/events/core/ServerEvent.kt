package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event

sealed class ServerEvent {
    class Connect : Event()

    class Disconnect : Event()
}