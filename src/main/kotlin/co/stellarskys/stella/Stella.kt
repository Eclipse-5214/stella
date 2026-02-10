package co.stellarskys.stella

import co.stellarskys.stella.managers.feature.FeatureManager
import co.stellarskys.stella.utils.animation.DeltaTracker
import co.stellarskys.stella.utils.render.nvg.NVGSpecialRenderer
import net.fabricmc.api.ClientModInitializer
import net.fabricmc.fabric.api.client.rendering.v1.SpecialGuiElementRegistry
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object Stella: ClientModInitializer {
    @JvmStatic val LOGGER: Logger = LogManager.getLogger("stella")
    @JvmStatic val NAMESPACE: String = "stella"
    @JvmStatic val PREFIX: String = "§7[§dStella§7]"
    @JvmStatic val SHORTPREFIX: String = "§d[SA]"
    @JvmStatic val DELTA: DeltaTracker = DeltaTracker()

    override fun onInitializeClient() {
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()

        SpecialGuiElementRegistry.register { NVGSpecialRenderer(it.vertexConsumers()) }
    }
}

