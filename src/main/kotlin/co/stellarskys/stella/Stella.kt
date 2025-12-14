package co.stellarskys.stella

import co.stellarskys.stella.managers.feature.FeatureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger

object Stella: ClientModInitializer {
    @JvmStatic val LOGGER: Logger = LogManager.getLogger("stella")
    @JvmStatic val NAMESPACE: String = "stella"
    @JvmStatic val PREFIX: String = "§7[§dStella§7]"
    @JvmStatic val SHORTPREFIX: String = "§d[SA]"
    @JvmStatic val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onInitializeClient() {
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()
    }
}

