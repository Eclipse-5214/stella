package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import net.minecraft.command.ICommandSender

@Stella.Command
object MainCommand: CommandUtils(
    "stella",
    "Opens the Config",
    listOf("sa", "sta")
) {
    override fun processCommand(sender: ICommandSender?, args: Array<out String?>?) {
        TickUtils.schedule(1){
            config.open()
        }
    }
}