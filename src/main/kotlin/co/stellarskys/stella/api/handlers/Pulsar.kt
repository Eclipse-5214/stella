package co.stellarskys.stella.api.handlers

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Module
import co.stellarskys.stella.api.handlers.Signal.onHover
import co.stellarskys.stella.events.EventBus
import co.stellarskys.stella.events.core.ServerEvent
import co.stellarskys.stella.managers.FeatureManager
import co.stellarskys.stella.utils.config
import dev.deftu.textile.Text

@Module
object Pulsar {
    private var shown = false

    val FirstInstallStore = Capsule("firstInstall", true)
    var FirstInstall: Boolean
        get() = FirstInstallStore.getData()
        set(value) = FirstInstallStore.setData(value)

    init {
        EventBus.on<ServerEvent.Connect> {
            if(!shown) {
                val showMessage = config["loadMessage"] as Boolean
                val loadMessage = Text
                    .literal("${Stella.PREFIX} §bMod loaded.")
                    .onHover("§b${FeatureManager.moduleCount} §dmodules §8- §b${FeatureManager.loadTime}§dms §8- §b${FeatureManager.commandCount} §dcommands")
                if (showMessage) Signal.fakeMessage(loadMessage)
                shown = true
            }

            if (FirstInstall) {
                Chronos.Tick.after(20 * 1) run {
                    Signal.fakeMessage(
                                "§b§l---------------------------------------------\n" +
                                "   §r§6Thank you for installing §d§lStella§r§3!\n" +
                                "\n" +
                                "   §r§3Commands\n" +
                                "   §r§d/sa help §3§l- §r§bFor a list of commands!\n" +
                                "\n" +
                                "   §r§dGithub:  https://github.com/Eclipse-5214/stella\n" +
                                "   §r§dDiscord: https://discord.gg/EzEfQyGdAg\n" +
                                "§b§l---------------------------------------------"
                    )

                    FirstInstall = false
                }
            }
        }
    }
}