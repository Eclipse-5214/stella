@file:OptIn(ExperimentalTime::class)
@file:Suppress("UNUSED")

package co.stellarskys.stella.utils.skyblock.location

import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.TablistEvent
import net.hypixel.data.type.GameType
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.anyMatch
import tech.thatgravyboat.skyblockapi.utils.regex.RegexUtils.findGroup
import tech.thatgravyboat.skyblockapi.utils.text.TextProperties.stripped
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.events.core.ScoreboardEvent
import co.stellarskys.stella.events.core.ServerEvent
import co.stellarskys.stella.utils.WorldUtils
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Slightly modified version of SkyblockAPI's LocationAPI to allow us to modify it to fit our needs.
 *
 * Original File: [GitHub](https://github.com/SkyblockAPI/SkyblockAPI/blob/2.0/src/common/main/kotlin/tech/thatgravyboat/skyblockapi/api/location/LocationAPI.kt)
 * @author SkyblockAPI
 */
@Module
object LocationAPI {
    private val unknownAreas = mutableMapOf<String, SkyBlockIsland?>()

    private val locationRegex = Regex(" *[⏣ф] *(?<location>(?:\\s?[^ൠ\\s]+)*)(?: ൠ x\\d)?")
    private val guestRegex = Regex("^ *\u270C *\\((?<guests>\\d+)/(?<max>\\d+)\\) *$")
    private val playerCountRegex = Regex(" *(?:players|party) \\((?<count>\\d+)\\) *")

    var forceOnSkyblock: Boolean = false

    var isOnSkyBlock: Boolean = false
        get() = field || forceOnSkyblock
        private set(value) {
            if (field != value) {
                field = value
                if (value) EventBus.post(LocationEvent.SkyblockJoin()) else EventBus.post(LocationEvent.SkyblockLeave())
            }
        }

    var island: SkyBlockIsland? = null
        private set

    var area: SkyBlockArea = SkyBlockAreas.NONE
        private set

    var serverId: String? = null
        private set

    var isGuest: Boolean = false
        private set

    var onHypixel: Boolean = false
        private set

    var onAlpha: Boolean = false
        private set

    var playerCount: Int = 0
        get() = field.coerceAtLeast(WorldUtils.players.size)
        private set

    val maxPlayerCount: Int?
        get() = when {
            serverId?.startsWith("mega") == true -> 60
            else -> when (island) {
                SkyBlockIsland.PRIVATE_ISLAND, SkyBlockIsland.GARDEN -> null
                SkyBlockIsland.KUUDRA -> 4
                SkyBlockIsland.MINESHAFT -> 4
                SkyBlockIsland.THE_CATACOMBS -> 5
                SkyBlockIsland.BACKWATER_BAYOU -> 16
                SkyBlockIsland.HUB -> 26
                SkyBlockIsland.JERRYS_WORKSHOP -> 27
                SkyBlockIsland.DARK_AUCTION -> 30
                else -> 24
            }
        }

    var lastServerChange: Instant = Instant.DISTANT_PAST
        private set

    init {
        EventBus.register<LocationEvent.ServerChange> { event ->
            lastServerChange = Clock.System.now()
            isOnSkyBlock = event.type == GameType.SKYBLOCK

            val newIsland = if (isOnSkyBlock && event.mode != null) SkyBlockIsland.Companion.getById(event.mode) else null
            val oldIsland = island

            island = newIsland
            EventBus.post(LocationEvent.IslandChange(oldIsland, newIsland))
            serverId = event.name
        }

        EventBus.registerIn<TablistEvent.Change> (skyblockOnly = true) { event ->
            val component = event.new.firstOrNull()?.firstOrNull() ?: return@registerIn
            playerCount = playerCountRegex.findGroup(component.stripped.lowercase(), "count")?.toIntOrNull() ?: 0
        }

        EventBus.registerIn<ScoreboardEvent.UpdateTitle> (skyblockOnly = true) { event ->
            isGuest = event.new.contains("guest", ignoreCase = true)
        }

        EventBus.registerIn<ScoreboardEvent.Update> (skyblockOnly = true) { event ->
            locationRegex.anyMatch(event.added, "location") { (location) ->
                val old = area
                area = SkyBlockArea(location)
                EventBus.post(LocationEvent.AreaChange(old, area))

                val knownArea = SkyBlockAreas.registeredAreas.entries.find { it.value.name == location } != null
                if (!knownArea) {
                    unknownAreas.putIfAbsent(location, island)
                }
            }

            guestRegex.anyMatch(event.added, "guests") { (current) ->
                playerCount = current.toIntOrNull() ?: 0
            }
        }

        EventBus.register<ServerEvent.Disconnect> {
            reset()
        }
    }

    private fun reset() {
        isOnSkyBlock = false
        isGuest = false
        onHypixel = false
        onAlpha = false
        serverId = null
        area = SkyBlockAreas.NONE

        val old = island
        island = null

        if (old != null) {
            EventBus.post(LocationEvent.IslandChange(old, null))
        }
    }
}