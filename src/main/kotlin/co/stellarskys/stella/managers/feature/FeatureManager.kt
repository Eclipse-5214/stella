package co.stellarskys.stella.managers.feature

import co.stellarskys.stella.Stella
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.generated.GeneratedFeatureRegistry
import co.stellarskys.stella.events.core.LocationEvent
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.utils.config
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback
import java.util.concurrent.ConcurrentHashMap

object FeatureManager {
    var moduleCount = 0
        private set
    var commandCount = 0
        private set
    var loadTime: Long = 0
        private set

    private val configListeners = ConcurrentHashMap<String, MutableList<Feature>>()
    private val pendingFeatures = ArrayList<Feature>()
    private val islandFeatures = ArrayList<Feature>()
    private val areaFeatures = ArrayList<Feature>()
    private val skyblockFeatures = ArrayList<Feature>()
    private val dungeonFloorFeatures = ArrayList<Feature>()
    val features = ArrayList<Feature>()

    init {
        EventBus.on<LocationEvent.SkyblockJoin> { for (f in skyblockFeatures) f.update() }
        EventBus.on<LocationEvent.SkyblockLeave> { for (f in skyblockFeatures) f.update() }
        EventBus.on<LocationEvent.IslandChange> { for (f in islandFeatures) f.update() }
        EventBus.on<LocationEvent.AreaChange> { for (f in areaFeatures) f.update() }
        EventBus.on<LocationEvent.DungeonFloorChange> { for (f in dungeonFloorFeatures) f.update() }

        config.registerListener { name, _ ->
            configListeners[name]?.let { list ->
                for (f in list) f.update()
            }
        }
    }

    fun addFeature(feature: Feature) = pendingFeatures.add(feature)

    fun loadFeatures() {
        val startTime = System.currentTimeMillis()

        val modules = GeneratedFeatureRegistry.modules
        val commands = GeneratedFeatureRegistry.commands

        for (module in modules) {
            runCatching {
                // Force initialization to register the feature
                Class.forName(module.name)
                moduleCount++
                Stella.LOGGER.debug("Loaded module: ${module.name}")
            }.onFailure { e ->
                Stella.LOGGER.error("Error initializing module ${module.name}: $e")
            }
        }

        for (command in commands) {
            runCatching {
                ClientCommandRegistrationCallback.EVENT.register { dispatcher, _ ->
                    command.register(dispatcher)
                }
                commandCount++
                Stella.LOGGER.debug("Loaded command: ${command::class.java.name}")
            }.onFailure { e ->
                Stella.LOGGER.error("Error initializing command ${command::class.java.name}: $e")
            }
        }

        loadTime = System.currentTimeMillis() - startTime
    }

    fun registerListener(configName: String, feature: Feature) {
        configListeners.getOrPut(configName) { mutableListOf() }.add(feature)
    }

    fun initializeFeatures() {
        for (feature in pendingFeatures) {
            features.add(feature)
            if (feature.islands.isNotEmpty()) islandFeatures.add(feature)
            if (feature.areas.isNotEmpty()) areaFeatures.add(feature)
            if (feature.dungeonFloors.isNotEmpty()) dungeonFloorFeatures.add(feature)
            if (feature.skyblockOnly) skyblockFeatures.add(feature)

            feature.initialize()
            feature.configName?.let { registerListener(it, feature) }
            feature.update()
        }

        pendingFeatures.clear()
    }
}