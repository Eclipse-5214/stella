package co.stellarskys.stella.events.core

import xyz.meowing.knit.api.events.Event

sealed class ServerEvent {
    class Connect : Event()

    class Disconnect : Event()
}