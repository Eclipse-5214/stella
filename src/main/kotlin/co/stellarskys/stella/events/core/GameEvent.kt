package co.stellarskys.stella.events.core

import xyz.meowing.knit.api.events.Event

sealed class GameEvent {
    class Start : Event()
    class Stop : Event()

    sealed class ModInit {
        class Pre : Event()
        class Post : Event()
    }
}