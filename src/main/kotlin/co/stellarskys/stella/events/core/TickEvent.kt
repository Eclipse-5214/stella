package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event

sealed class TickEvent {
    class Client: Event()
    class Server: Event()
}