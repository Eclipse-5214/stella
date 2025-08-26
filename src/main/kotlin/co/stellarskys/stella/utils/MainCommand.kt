package co.stellarskys.stella.utils

import co.stellarskys.stella.Stella
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

@Stella.Command
object MainCommand : CommandUtils(
    "stella",
    listOf("sa", "sta")
) {
    override fun execute(context: CommandContext<FabricClientCommandSource>): Int {
        config.open()
        return 1
    }

    override fun buildCommand(builder: LiteralArgumentBuilder<FabricClientCommandSource>) {
        builder.then(
            ClientCommandManager.literal("hud")
                .executes { _ ->
                    TickUtils.schedule(1) {
                        Stella.mc.execute {
                            //Stella.mc.setScreen(HUDEditor())
                        }
                    }
                    1
                }
        )
    }
}