package co.stellarskys.stella.utils

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.features.msc.buttonUtils.ButtonLayoutEditor
import co.stellarskys.stella.hud.HUDEditor
import dev.deftu.omnicore.api.client.client
import dev.deftu.omnicore.api.scheduling.TickSchedulers

@Command
object MainCommand : Commodore("stella", "sta", "sa") {
    init {
        literal("hud") {
            runs {
                TickSchedulers.client.post {
                    client.setScreen(HUDEditor())
                }
            }
        }

        literal("buttons") {
            runs {
                TickSchedulers.client.post {
                    client.setScreen(ButtonLayoutEditor())
                }
            }
        }

        runs {
            config.open()
        }
    }
}