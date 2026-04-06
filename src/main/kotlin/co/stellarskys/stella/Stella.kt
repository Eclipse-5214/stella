package co.stellarskys.stella

import co.stellarskys.stella.managers.FeatureManager
import co.stellarskys.stella.api.animation.DeltaTracker
import co.stellarskys.stella.api.nvg.NVGPIPRenderer
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.TickEvent
import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlinx.coroutines.*
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry

object Stella: ClientModInitializer {
    @JvmStatic val LOGGER: Logger = LogManager.getLogger("stella")
    @JvmStatic val NAMESPACE: String = "stella"
    @JvmStatic val PREFIX: String = "§7[§dStella§7]"
    @JvmStatic val SHORTPREFIX: String = "§d[SA]"
    @JvmStatic val ETHER: String = "https://ether.stellarskys.co"
    @JvmStatic val API: String = "https://api.stellarskys.co"
    @JvmStatic val DELTA: DeltaTracker = DeltaTracker()
    @JvmStatic val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onInitializeClient() {
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()

        SpecialGuiElementRegistry.register { NVGPIPRenderer(it.vertexConsumers()) }
    }
}

