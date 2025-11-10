package co.stellarskys.stella.events.compat

import tech.thatgravyboat.skyblockapi.api.SkyBlockAPI
import tech.thatgravyboat.skyblockapi.api.events.base.Subscription
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardTitleUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.info.ScoreboardUpdateEvent
import tech.thatgravyboat.skyblockapi.api.events.info.TabListChangeEvent
import tech.thatgravyboat.skyblockapi.api.events.screen.PlayerHotbarChangeEvent
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.PlayerEvent
import co.stellarskys.stella.events.core.ScoreboardEvent
import co.stellarskys.stella.events.core.TablistEvent

/**
 * Handles and converts SkyblockAPI events to our own.
 */
@Module
object SkyblockAPI {
    init {
        SkyBlockAPI.eventBus.register(this)
    }

    @Subscription
    fun onTabListUpdate(event: TabListChangeEvent) {
        EventBus.post(TablistEvent.Change(event.old, event.new))
    }

    @Subscription
    fun onScoreboardTitleUpdate(event: ScoreboardTitleUpdateEvent) {
        EventBus.post(ScoreboardEvent.UpdateTitle(event.old, event.new))
    }

    @Subscription
    fun onScoreboardChange(event: ScoreboardUpdateEvent) {
        EventBus.post(ScoreboardEvent.Update(event.old, event.new, event.components))
    }

    @Subscription
    fun onPlayerHotbarUpdate(event: PlayerHotbarChangeEvent) {
        EventBus.post(PlayerEvent.HotbarChange(event.slot, event.item))
    }
}