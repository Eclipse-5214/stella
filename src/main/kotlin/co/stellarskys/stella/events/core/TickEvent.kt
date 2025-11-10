package co.stellarskys.stella.events.core

import xyz.meowing.knit.api.events.Event

sealed class TickEvent {
    class Client: Event()
    class Server: Event()
}