package co.stellarskys.stella.features

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.api.Event
import co.stellarskys.stella.events.api.EventHandle
import co.stellarskys.stella.managers.feature.FeatureManager
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import tech.thatgravyboat.skyblockapi.api.area.dungeon.DungeonFloor
import tech.thatgravyboat.skyblockapi.api.location.LocationAPI
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockArea
import tech.thatgravyboat.skyblockapi.api.location.SkyBlockIsland

open class Feature(
    val configName: String? = null,
    val skyblockOnly: Boolean = false,
    island: Any? = null,
    area: Any? = null,
    dungeonFloor: Any? = null
) {
    val events = ArrayList<EventHandle<*>>(4)
    private var isRegistered = false

    val islands: List<SkyBlockIsland> = when (island) {
        is SkyBlockIsland -> listOf(island)
        is List<*> -> island.filterIsInstance<SkyBlockIsland>()
        else -> emptyList()
    }

    val areas: List<SkyBlockArea> = when (area) {
        is SkyBlockArea -> listOf(area)
        is List<*> -> area.filterIsInstance<SkyBlockArea>()
        else -> emptyList()
    }

    val dungeonFloors: List<DungeonFloor> = when (dungeonFloor) {
        is DungeonFloor -> listOf(dungeonFloor)
        is List<*> -> dungeonFloor.filterIsInstance<DungeonFloor>()
        else -> emptyList()
    }

    init {
        FeatureManager.addFeature(this)
    }

    open fun initialize() {}
    open fun onRegister() {}
    open fun onUnregister() {}

    fun isEnabled(): Boolean {
        if (!(configName?.let { config.getValue<Boolean>(it) } ?: true)) return false
        if (skyblockOnly && !LocationAPI.isOnSkyBlock) return false
        if (islands.isNotEmpty() && LocationAPI.island !in islands) return false
        if (areas.isNotEmpty() && LocationAPI.area !in areas) return false
        if (dungeonFloors.isNotEmpty()) {
            if (LocationAPI.island != SkyBlockIsland.THE_CATACOMBS) return false
            if (Dungeon.floor !in dungeonFloors) return false
        }

        return true
    }

    fun update() = onToggle(isEnabled())

    open fun onToggle(state: Boolean) {
        if (state == isRegistered) return

        if (state) {
            for (e in events) e.register()
            onRegister()
            isRegistered = true
        } else {
            for (e in events) e.unregister()
            onUnregister()
            isRegistered = false
        }
    }

    inline fun <reified T : Event> on(noinline cb: (T) -> Unit) { events += EventBus.on<T>(register = false, handler = cb) }
}