package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.annotations.Command
import co.stellarskys.stella.api.dungeons.Dungeon
import co.stellarskys.stella.api.dungeons.score.DungeonScore
import co.stellarskys.stella.api.handlers.Atlas
import co.stellarskys.stella.api.handlers.Chronos
import co.stellarskys.stella.api.handlers.Signal
import co.stellarskys.stella.api.zenith.client
import co.stellarskys.stella.api.zenith.player
import co.stellarskys.stella.features.dungeons.JoinInfo
import co.stellarskys.stella.features.msc.ProfileViewer
import co.stellarskys.stella.features.msc.buttonUtils.ButtonLayoutEditor
import co.stellarskys.stella.features.secrets.utils.routes.RouteRecorder
import co.stellarskys.stella.hud.HUDEditor
@Command
object MainCommand : Atlas("stella", "sta", "sa") {
    init {
        literal("hud") {
            runs {
                Chronos.Tick post {
                    client.setScreen(HUDEditor())
                }
            }
        }

        literal("buttons") {
            runs {
                Chronos.Tick post {
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

            literal("save") {
                runs {
                    RouteRecorder.saveRoute()
                }
            }

            literal("reload") {
                runs {
                    RouteRecorder.reloadRoutes()
                }
            }

            literal("custom") {
                runs {
                    RouteRecorder.addCustom()
                }
            }

            literal("missing") {
                runs {
                    val missing = RouteRecorder.getMissing()
                    Signal.fakeMessage("${Stella.PREFIX} §bMissing Rooms §6${missing.size}§b:")
                    missing.forEach { Signal.fakeMessage("§7 - $it") }
                }
            }
        }

        literal("help") {
            runs {
                Signal.fakeMessage("§8§m------------------------------------------")
                Signal.fakeMessage("§6/stella §7main command! Aliases: §6/sa /sta")
                Signal.fakeMessage("§6/sa help §7Opens the Stella help menu!")
                Signal.fakeMessage("§6/sa hud §7Opens the HUD editor!")
                //Signal.fakeMessage("§6/stellaroutes §routes config! (if installed) Aliases: §6/sr /str")
                //Signal.fakeMessage("§6/sa route §7 route recording try §6/sa route help §7for more info!")
                Signal.fakeMessage("§8§m------------------------------------------")
            }
        }

        literal("dumpscore") {
            runs {
                if (Dungeon.floor == null) {
                    Signal.fakeMessage("${Stella.PREFIX} §cError: Not in dungeon")
                }


                Signal.fakeMessage("§d§m------------------------------------------")
                Signal.fakeMessage("§bDungeon Score Breakdown §7(§6${Dungeon.floor?.name ?: "?"}§7)")
                Signal.fakeMessage("§d§m------------------------------------------")
                Signal.fakeMessage("                 §bScore: §6${DungeonScore.score}")
                Signal.fakeMessage("")
                Signal.fakeMessage("§7Skill Score§8: §b${DungeonScore.skillScore}")
                Signal.fakeMessage("§7Explore Score§8: §b${DungeonScore.exploreScore}")
                Signal.fakeMessage("§7Speed Score§8: §b${DungeonScore.speedScore}")
                Signal.fakeMessage("§7Bonus Score§8: §b${DungeonScore.bonusScore}")
                Signal.fakeMessage("")
                Signal.fakeMessage("§d§m------------------------------------------")
            }
        }

        literal("cata") {
            runs<Greedy?> ("name") { arg ->
                val name = arg?.string ?: player?.name?.string ?: return@runs
                JoinInfo.fetchAndDisplayStats(name)
            }
        }

        literal("pv") {
            runs<Greedy?> ("name") { arg ->
                val name = arg?.string ?: player?.name?.string ?: return@runs
                ProfileViewer.view(name)
            }
        }

        runs {
            config.open()
        }
    }
}