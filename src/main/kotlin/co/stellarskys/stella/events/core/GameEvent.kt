package co.stellarskys.stella.events.core

import co.stellarskys.stella.api.events.Event

sealed class GameEvent {
    class Start : Event()
    class Stop : Event()

    sealed class ModInit {
        class Pre : Event()
        class Post : Event()
    }
}