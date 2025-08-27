package co.stellarskys.stella

import co.stellarskys.stella.events.*
import co.stellarskys.stella.features.Feature
import co.stellarskys.stella.features.FeatureLoader
import co.stellarskys.stella.utils.ChatUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.config
import java.util.concurrent.ConcurrentHashMap

//#if FABRIC
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents
import net.minecraft.client.MinecraftClient
import net.minecraft.client.gui.screen.ingame.InventoryScreen
//#elseif FORGE
//#if MC >= 1.16.5
//$$ import net.minecraftforge.eventbus.api.IEventBus
//$$ import net.minecraftforge.fml.common.Mod
//$$ import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
//$$ import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
//$$ import net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent
//$$ import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext
//$$ import net.minecraft.client.Minecraft
//#else
//$$ import net.minecraftforge.fml.common.Mod
//$$ import net.minecraftforge.fml.common.Mod.EventHandler
//$$ import net.minecraftforge.fml.common.event.FMLInitializationEvent
//$$ import net.minecraft.client.Minecraft
//$$ import net.minecraft.client.gui.inventory.GuiInventory
//$$
//#endif
//#endif

//#if FORGE-LIKE
//#if MC >= 1.16.5
//$$ @Mod("stella")
//#else
//$$ @Mod(modid = "stella", version = "1.0.0", useMetadata = true, clientSideOnly = true)
//#endif
//#endif
class Stella
    //#if FABRIC
    : ClientModInitializer
    //#endif
{
    private var shown = false

    //#if MC == 1.8.9
    //$$ private var eventCall: EventBus.EventCall? = null
    //#endif

    @Target(AnnotationTarget.CLASS)
    annotation class Module

    @Target(AnnotationTarget.CLASS)
    annotation class Command

    //#if FABRIC
    override
    //#elseif MC == 1.8.9
    //$$ @Mod.EventHandler
    //#endif
    fun onInitializeClient(
        //#if FORGE-LIKE
        //#if MC >= 1.16.5
        //$$ event: FMLClientSetupEvent
        //#else
        //$$ event: FMLInitializationEvent
        //#endif
        //#endif
    ) {
        //#if MC == 1.8.9
        //$$ EventBus.post(GameEvent.Load())
        //#endif

        init()
        FeatureLoader.init()

        //#if MC >= 1.21.5
        ClientPlayConnectionEvents.JOIN.register { _, _, _ ->
            if (shown) return@register

            ChatUtils.addMessage(
                "$PREFIX §fMod loaded.",
                "§c${FeatureLoader.getFeatCount()} modules §8- §c${FeatureLoader.getLoadtime()}ms §8- §c${FeatureLoader.getCommandCount()} commands"
            )
        }
        //#elseif MC == 1.8.9
        //$$ eventCall = EventBus.register<EntityEvent.Join> ({ event ->
        //$$    if (event.entity == mc.thePlayer) {
        //$$        ChatUtils.addMessage(
        //$$            "$PREFIX §fMod loaded.",
        //$$            "§c${FeatureLoader.getFeatCount()} modules §8- §c${FeatureLoader.getLoadtime()}ms §8- §c${FeatureLoader.getCommandCount()} commands"
        //$$        )
        //$$        eventCall?.unregister()
        //$$        eventCall = null
        //$$    }
        //$$ })
        //#endif

        config.registerListener{ name, value ->
            configListeners[name]?.forEach { it.update() }
            ConfigCallback[name]?.forEach { it() }
        }

        EventBus.register<GuiEvent.Open> ({ event ->
            //#if MC >= 1.21.5
            if (event.screen is InventoryScreen) isInInventory = true
            //#elseif MC == 1.8.9
            //$$ if (event.screen is GuiInventory) isInInventory = true
            //#endif
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
        private val features = mutableListOf<Feature>()
        private val configListeners = ConcurrentHashMap<String, MutableList<Feature>>()
        private val ConfigCallback = ConcurrentHashMap<String, MutableList<() -> Unit>>()
        private val areaFeatures = mutableListOf<Feature>()
        private val subareaFeatures = mutableListOf<Feature>()

        val mc = MinecraftClient.getInstance()
        val NAMESPACE: String = "stella"
        val INSTANCE: Stella? = null
        val PREFIX: String = "§d[Stella]"
        val SHORTPREFIX: String = "§d[SA]"

        var isInInventory = false

        fun addFeature(feature: Feature) {
            features.add(feature)

            if (feature.hasAreas()) areaFeatures.add(feature)
            if (feature.hasSubareas()) subareaFeatures.add(feature)
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

        //fun getResource(path: String) = Identifier.of(NAMESPACE, path)

        fun init() {}
    }
}