package co.stellarskys.stella

import co.stellarskys.stella.events.*
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.FeatureLoader
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.NEUApi
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner
import co.stellarskys.stella.utils.skyblock.dungeons.RoomRegistry
import java.util.concurrent.ConcurrentHashMap
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import org.apache.logging.log4j.LogManager

class Stella: ClientModInitializer {
    private var shown = false

    @Target(AnnotationTarget.CLASS)
    annotation class Module

    @Target(AnnotationTarget.CLASS)
    annotation class Command

    override
    fun onInitializeClient() {
        init()
        FeatureLoader.init()
        RoomRegistry.loadFromRemote()
        DungeonScanner.init()
        NEUApi.init()

        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (shown) return@register

            ChatUtils.addMessage(
                "$PREFIX §fMod loaded.",
                "§b${FeatureLoader.getFeatCount()} §dmodules §8- §b${FeatureLoader.getLoadtime()}§dms §8- §b${FeatureLoader.getCommandCount()} §dcommands"
            )

            shown = true
        }

        config.registerListener{ name, value ->
            configListeners[name]?.forEach { it.update() }
            ConfigCallback[name]?.forEach { it() }
        }

        EventBus.register<GuiEvent.Open> ({ event ->
            if (event.screen is InventoryScreen) isInInventory = true
        })

        EventBus.register<GuiEvent.Close> ({
            isInInventory = false
        })

        EventBus.register<AreaEvent.Main> ({
            TickUtils.scheduleServer(1) {
                areaFeatures.forEach { it.update() }
            }
        })

        EventBus.register<AreaEvent.Sub> ({
            TickUtils.scheduleServer(1) {
                subareaFeatures.forEach { it.update() }
            }
        })
    }

    companion object {
        private val pendingFeatures = mutableListOf<Feature>()
        private val features = mutableListOf<Feature>()
        private val configListeners = ConcurrentHashMap<String, MutableList<Feature>>()
        private val ConfigCallback = ConcurrentHashMap<String, MutableList<() -> Unit>>()
        private val areaFeatures = mutableListOf<Feature>()
        private val subareaFeatures = mutableListOf<Feature>()

        @JvmField val LOGGER = LogManager.getLogger("stella")
        val mc = MinecraftClient.getInstance()
        val NAMESPACE: String = "stella"
        val INSTANCE: Stella? = null
        val PREFIX: String = "§7[§dStella§7]"
        val SHORTPREFIX: String = "§d[SA]"

        var isInInventory = false

        fun addFeature(feature: Feature) = pendingFeatures.add(feature)

        fun initializeFeatures() {
            pendingFeatures.forEach { feature ->
                features.add(feature)
                if (feature.hasAreas()) areaFeatures.add(feature)
                if (feature.hasSubareas()) subareaFeatures.add(feature)
                feature.initialize()
                feature.configName?.let { registerListener(it, feature) }
                feature.update()
            }
            pendingFeatures.clear()
        }

        fun registerListener(configName: String, feature: Feature) {
            configListeners.getOrPut(configName) { mutableListOf() }.add(feature)
        }

        fun registerListener(configName: String, callback: () -> Unit) {
            ConfigCallback.getOrPut(configName) { mutableListOf() }.add(callback)
        }

        //fun getResource(path: String) = Identifier.of(NAMESPACE, path)

        fun init() {}
    }
}