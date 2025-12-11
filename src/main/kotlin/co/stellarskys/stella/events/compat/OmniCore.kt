package co.stellarskys.stella.events.compat

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.TickEvent
import dev.deftu.omnicore.api.eventBus
import dev.deftu.eventbus.on
import dev.deftu.omnicore.api.client.events.ClientTickEvent
import dev.deftu.omnicore.api.events.ServerTickEvent

/**
 * Handles and converts OmniCore's events to our own.
 */
@Module
object OmniCore {
    init {
        eventBus.on<ServerTickEvent.Pre> {
            EventBus.post(TickEvent.Server())
        }

        eventBus.on<ClientTickEvent.Pre> {
            EventBus.post(TickEvent.Client())
        }
    }
}