package co.stellarskys.stella.events.core

import xyz.meowing.knit.api.events.Event

sealed class GameEvent {
    /**
     * Posted when the game has started, and it's about to tick for the first time.
     *
     * This occurs while the splash screen is displayed.
     * @see xyz.meowing.krypt.events.EventBus
     * @since 1.0.0
     */
    class Start : Event()

    /**
     * Posted when the game begins to stop.
     *
     * This will be called before the integrated server is stopped if it is running.
     * @see xyz.meowing.krypt.events.EventBus
     * @since 1.0.0
     */
    class Stop : Event()

    sealed class ModInit {
        /**
         * Posted before the mod has initialized.
         *
         * @see xyz.meowing.krypt.Krypt
         * @since 1.2.0
         */
        class Pre : Event()

        /**
         * Posted after the mod has initialized.
         *
         * @see xyz.meowing.krypt.Krypt
         * @since 1.2.0
         */
        class Post : Event()
    }
}