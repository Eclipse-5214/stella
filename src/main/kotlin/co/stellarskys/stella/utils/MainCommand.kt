package co.stellarskys.stella.utils

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.features.msc.buttonUtils.ButtonLayoutEditor
import co.stellarskys.stella.hud.HUDEditor
import xyz.meowing.knit.api.KnitClient
import xyz.meowing.knit.api.command.Commodore
import xyz.meowing.knit.api.scheduler.TickScheduler

@Command
object MainCommand : Commodore("stella", "sa", "sta") {
    init {
        literal("hud") {
            runs {
                TickScheduler.Client.post {
                    KnitClient.client.setScreen(HUDEditor())
                }
            }
        }

        literal("buttons") {
            runs {
                TickScheduler.Client.post {
                    KnitClient.client.setScreen(ButtonLayoutEditor())
                }
            }
        }

        runs {
            config.open()
        }
    }
}