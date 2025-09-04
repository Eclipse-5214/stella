package co.stellarskys.stella

import co.stellarskys.stella.events.*
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.FeatureLoader
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.config
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner
import co.stellarskys.stella.utils.skyblock.dungeons.RoomRegistry
import java.util.concurrent.ConcurrentHashMap
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraft.client.Minecraft
import net.minecraft.client.entity.AbstractClientPlayer
import net.minecraft.client.gui.GuiMainMenu
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.client.renderer.entity.RenderPlayer
import net.minecraft.client.resources.DefaultPlayerSkin
import net.minecraft.entity.player.EnumPlayerModelParts
import net.minecraftforge.fml.common.event.FMLServerStoppingEvent
import java.io.File

@Mod(modid = "stella", version = "1.0.0", useMetadata = true, clientSideOnly = true)
class Stella {
    private var shown = false
    private var eventCall: EventBus.EventCall? = null

    @Target(AnnotationTarget.CLASS)
    annotation class Module

    @Target(AnnotationTarget.CLASS)
    annotation class Command

    @EventHandler
    fun onInitializeClient(event: FMLInitializationEvent) {
        EventBus.post(GameEvent.Load())

        init()
        FeatureLoader.init()
        RoomRegistry.loadFromRemote()
        DungeonScanner.init()

        eventCall = EventBus.register<EntityEvent.Join> ({ event ->
            if (event.entity == mc.thePlayer) {
                ChatUtils.addMessage(
                    "$PREFIX §fMod loaded.",
                    "§c${FeatureLoader.getFeatCount()} modules §8- §c${FeatureLoader.getLoadtime()}ms §8- §c${FeatureLoader.getCommandCount()} commands"
                )
                eventCall?.unregister()
                eventCall = null
            }
        })

        config.registerListener{ name, value ->
            configListeners[name]?.forEach { it.update() }
            ConfigCallback[name]?.forEach { it() }
        }

        EventBus.register<GuiEvent.Open> ({ event ->
            if (event.screen is GuiInventory) isInInventory = true
            if (event.screen is GuiMainMenu && !shown) EventBus.post(GameEvent.Unload())
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

        printDiscoveredSprites()
    }


    companion object {
        private val pendingFeatures = mutableListOf<Feature>()
        private val features = mutableListOf<Feature>()
        private val configListeners = ConcurrentHashMap<String, MutableList<Feature>>()
        private val ConfigCallback = ConcurrentHashMap<String, MutableList<() -> Unit>>()
        private val areaFeatures = mutableListOf<Feature>()
        private val subareaFeatures = mutableListOf<Feature>()

        val mc = Minecraft.getMinecraft()
        val NAMESPACE: String = "stella"
        val INSTANCE: Stella? = null
        val PREFIX: String = "§d[Stella]"
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

        fun updateFeatures() {
            features.forEach { it.update() }
        }

        fun listSpriteResources(): List<String> {
            val path = "assets/stella/textures/gui/sprites/stellanav"
            val loader = Stella::class.java.classLoader
            val url = loader.getResource(path) ?: return emptyList()

            return File(url.toURI()).listFiles()
                ?.filter { it.extension == "png" }
                ?.map { it.name }
                ?: emptyList()
        }

        fun printDiscoveredSprites() {
            val sprites = listSpriteResources() // or listSpriteResources() in dev
            println("Discovered ${sprites.size} sprite(s):")
            sprites.forEach { println("- $it") }
        }

        //fun getResource(path: String) = Identifier.of(NAMESPACE, path)

        fun init() {}

    }
}