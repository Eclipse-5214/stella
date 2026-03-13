package co.stellarskys.stella

import co.stellarskys.stella.managers.FeatureManager
import co.stellarskys.stella.api.animation.DeltaTracker
import co.stellarskys.stella.api.nvg.NVGPIPRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import kotlinx.coroutines.*

object Stella: ClientModInitializer {
    @JvmStatic val LOGGER: Logger = LogManager.getLogger("stella")
    @JvmStatic val NAMESPACE: String = "stella"
    @JvmStatic val PREFIX: String = "§7[§dStella§7]"
    @JvmStatic val SHORTPREFIX: String = "§d[SA]"
    @JvmStatic val DELTA: DeltaTracker = DeltaTracker()
    @JvmStatic val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onInitializeClient() {
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()

        SpecialGuiElementRegistry.register { NVGPIPRenderer(it.vertexConsumers()) }
    }
}

