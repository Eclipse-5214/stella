package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import co.stellarskys.stella.Stella.Companion.mc
import co.stellarskys.stella.hud.HUDEditor
import co.stellarskys.stella.utils.CommandUtils
import co.stellarskys.stella.utils.TickUtils
import co.stellarskys.stella.utils.skyblock.dungeons.DungeonScanner
import net.minecraft.command.ICommandSender
import net.minecraft.util.BlockPos

@Stella.Command
object MainCommand: CommandUtils(
    "stella",
    "Opens the Config",
    listOf("sa", "sta")
) {
    override fun processCommand(sender: ICommandSender?, args: Array<out String?>?) {
        if (args != null && args.isNotEmpty()) {
            when (args[0]?.lowercase()) {
                "hud" -> {
                    TickUtils.schedule(1) {
                        mc.displayGuiScreen(HUDEditor())
                    }
                    return
                }
            }
        }

        TickUtils.schedule(1){
            config.open()
        }
    }

    override fun getTabCompletions(sender: ICommandSender, args: Array<String>, pos: BlockPos): List<String> {
        if (args.size == 1) return listOf("hud").filter { it.startsWith(args[0].lowercase()) }
        return emptyList()
    }
}

@Stella.Command
object dhdebug: CommandUtils(
    "sadb",
    "Opens the Config"
) {
    override fun processCommand(sender: ICommandSender?, args: Array<out String?>?) {
        val room = DungeonScanner.currentRoom ?: return
        val name = room.name ?: "Unnamed"
        val cores = room.cores

        if (cores.isEmpty()) {
            ChatUtils.addMessage("${Stella.PREFIX} §b$name §fhas no scanned cores!")
        } else {
            ChatUtils.addMessage("${Stella.PREFIX} §b$name §fcore hash${if (cores.size > 1) "es" else ""}:")
            cores.forEach {
                ChatUtils.addMessage(" - $it")
            }

            ChatUtils.addMessage("${Stella.PREFIX} §b$name §ftype is §b${room.type}§f, with a checkmark of §b${room.checkmark}§f, explored? ${room.explored}")
        }
    }
}