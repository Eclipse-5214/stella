package co.stellarskys.stella.utils

import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.features.msc.buttonUtils.ButtonLayoutEditor
import co.stellarskys.stella.features.secrets.utils.RouteRecorder
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

        literal("route") {
            literal("start") {
                runs {
                    RouteRecorder.startRecording()
                }
            }

            literal("stop") {
                runs {
                    RouteRecorder.stopRecording()
                }
            }

            /*
            literal("reload") {
                runs {
                    WaypointRegistry.reloadFromLocal(notifyUser = true)
                    RoomWaypointHandler.reloadCurrentRoom()
                }
            }
             */
        }


        literal("help") {
            runs {
                ChatUtils.fakeMessage("§8§m------------------------------------------");
                ChatUtils.fakeMessage("§6/stella §7main command! Aliases: §6/sa /sta");
                ChatUtils.fakeMessage("§6/sa help §7Opens the Stella help menu!");
                ChatUtils.fakeMessage("§6/sa hud §7Opens the HUD editor!");
                //ChatUtils.fakeMessage("§6/stellaroutes §routes config! (if installed) Aliases: §6/sr /str");
                //ChatUtils.fakeMessage("§6/sa route §7 route recording try §6/sa route help §7for more info!");
                ChatUtils.fakeMessage("§8§m------------------------------------------");
            }
        }

        runs {
            config.open()
        }
    }
}