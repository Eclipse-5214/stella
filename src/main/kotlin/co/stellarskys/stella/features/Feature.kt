@file:Suppress("UNUSED")

package co.stellarskys.stella.features

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.api.Event
import co.stellarskys.stella.events.api.EventCall
import co.stellarskys.stella.managers.feature.FeatureManager
import co.stellarskys.stella.utils.skyblock.location.SkyBlockArea
import co.stellarskys.stella.utils.skyblock.location.SkyBlockIsland
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.dungeons.Dungeon
import co.stellarskys.stella.utils.skyblock.dungeons.utils.DungeonFloor
import co.stellarskys.stella.utils.skyblock.location.LocationAPI
import dev.deftu.omnicore.api.scheduling.TickScheduler
import dev.deftu.omnicore.api.scheduling.TickSchedulers

open class Feature(
    val configName: String? = null,
    val skyblockOnly: Boolean = false,
    island: Any? = null,
    area: Any? = null,
    dungeonFloor: Any? = null
) {
    val events = mutableListOf<EventCall>()
    val tickHandles = mutableSetOf<TickScheduler.Handle>()
    val namedEventCalls = mutableMapOf<String, EventCall>()
    private var setupLoops: (() -> Unit)? = null
    private var isRegistered = false

    private val islands: List<SkyBlockIsland> = when (island) {
        is SkyBlockIsland -> listOf(island)
        is List<*> -> island.filterIsInstance<SkyBlockIsland>()
        else -> emptyList()
    }

    private val areas: List<SkyBlockArea> = when (area) {
        is SkyBlockArea -> listOf(area)
        is List<*> -> area.filterIsInstance<SkyBlockArea>()
        else -> emptyList()
    }


    private val dungeonFloors: List<DungeonFloor> = when (dungeonFloor) {
        is DungeonFloor -> listOf(dungeonFloor)
        is List<*> -> dungeonFloor.filterIsInstance<DungeonFloor>()
        else -> emptyList()
    }

    init {
        FeatureManager.addFeature(this)
    }

    private val configValue: () -> Boolean = {
        configName?.let { config.getValue<Boolean>(it) } ?: true
    }

    open fun initialize() {}

    protected fun setupLoops(block: () -> Unit) {
        setupLoops = block
    }

    open fun onRegister() {
        setupLoops?.invoke()
    }

    open fun onUnregister() {
        cancelLoops()
    }

    open fun addConfig() {}

    fun isEnabled(): Boolean = configValue() && inSkyblock() && inArea() && inSubarea() //&& inDungeonFloor()

    fun update() = onToggle(isEnabled())

    @Synchronized
    open fun onToggle(state: Boolean) {
        if (state == isRegistered) return

        if (state) {
            events.forEach { it.register() }
            onRegister()
            isRegistered = true
        } else {
            events.forEach { it.unregister() }
            onUnregister()
            isRegistered = false
        }
    }

    inline fun <reified T : Event> register(priority: Int = 0, noinline cb: (T) -> Unit) {
        events.add(EventBus.register<T>(priority, false, cb))
    }

    inline fun <reified T : Event> createCustomEvent(name: String, priority: Int = 0, noinline cb: (T) -> Unit) {
        val eventCall = EventBus.register<T>(priority, false, cb)
        namedEventCalls[name] = eventCall
    }

    fun registerEvent(name: String) {
        namedEventCalls[name]?.register()
    }

    fun unregisterEvent(name: String) {
        namedEventCalls[name]?.unregister()
    }

    inline fun <reified T> loop(intervalTicks: Int, noinline action: () -> Unit): Any {
        return when (T::class) {
            ClientTick::class -> {
                val handle = TickSchedulers.client.every(intervalTicks, runnable = action)
                tickHandles.add(handle)
                handle
            }
            ServerTick::class -> {
                val handle = TickSchedulers.server.every(intervalTicks, runnable = action)
                tickHandles.add(handle)
                handle
            }
            else -> throw IllegalArgumentException("Unsupported loop type: ${T::class}")
        }
    }

    private fun cancelLoops() {
        tickHandles.forEach { it.cancel() }
        tickHandles.clear()
    }

    fun inSkyblock(): Boolean = !skyblockOnly || LocationAPI.isOnSkyBlock

    fun inArea(): Boolean = islands.isEmpty() || LocationAPI.island in islands

    fun inSubarea(): Boolean = areas.isEmpty() || LocationAPI.area in areas

    fun inDungeonFloor(): Boolean {
        if (dungeonFloors.isEmpty()) return true
        return SkyBlockIsland.THE_CATACOMBS.inIsland() && Dungeon.floor in dungeonFloors
    }

    fun hasIslands(): Boolean = islands.isNotEmpty()

    fun hasAreas(): Boolean = areas.isNotEmpty()

    fun hasDungeonFloors(): Boolean = dungeonFloors.isNotEmpty()
}

class ClientTick
class ServerTick
class Timer