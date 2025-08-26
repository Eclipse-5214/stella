package co.stellarskys.stella

import java.util.concurrent.ConcurrentHashMap

//#if FABRIC
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.ModContainer
import net.minecraft.client.MinecraftClient
import net.minecraft.util.Identifier
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
    @Target(AnnotationTarget.CLASS)
    annotation class Module

    @Target(AnnotationTarget.CLASS)
    annotation class Command

    //#if FABRIC
    override
    //#endif
    fun onInitializeClient() {
    }

    companion object {
        //private val features = mutableListOf<Feature>()
        //private val configListeners = ConcurrentHashMap<String, MutableList<Feature>>()
        private val ConfigCallback = ConcurrentHashMap<String, MutableList<() -> Unit>>()
        //private val areaFeatures = mutableListOf<Feature>()
        //private val subareaFeatures = mutableListOf<Feature>()

        val mc = MinecraftClient.getInstance()
        val NAMESPACE: String = "stella"
        val INSTANCE: Stella? = null
        val PREFIX: String = "§d[Stella]"
        val SHORTPREFIX: String = "§d[SA]"

        var isInInventory = false

        /*
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

        fun getResource(path: String) = Identifier.of(NAMESPACE, path)
         */
        fun init() {}
    }
}