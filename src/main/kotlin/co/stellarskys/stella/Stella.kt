package co.stellarskys.stella

import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.ServerEvent
import co.stellarskys.stella.managers.feature.FeatureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.fabricmc.api.ClientModInitializer
import org.apache.logging.log4j.LogManager
import xyz.meowing.knit.api.KnitChat
import xyz.meowing.knit.api.text.KnitText

object Stella: ClientModInitializer {
    private var shown = false

    @JvmStatic val LOGGER = LogManager.getLogger("stella")
    @JvmStatic val NAMESPACE: String = "stella"
    @JvmStatic val PREFIX: String = "§7[§dStella§7]"
    @JvmStatic val SHORTPREFIX: String = "§d[SA]"
    @JvmStatic val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    override fun onInitializeClient() {
        FeatureManager.loadFeatures()
        FeatureManager.initializeFeatures()
        EventBus.register<ServerEvent.Connect> {
            if (shown) return@register

            val loadMessage = KnitText
                .literal("$PREFIX §bMod loaded.")
                .onHover("§b${FeatureManager.moduleCount} §dmodules §8- §b${FeatureManager.loadTime}§dms §8- §b${FeatureManager.commandCount} §dcommands")

            KnitChat.fakeMessage(loadMessage)

            shown = true
        }
    }
}
