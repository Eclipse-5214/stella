package co.stellarskys.stella.events.core

import co.stellarskys.stella.events.api.Event

sealed class TickEvent {
    class Client: Event()
    class Server: Event()
}