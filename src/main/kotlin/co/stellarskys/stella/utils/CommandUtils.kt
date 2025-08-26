package co.stellarskys.stella.utils

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource

abstract class CommandUtils(
    private val name: String,
    private val aliases: List<String> = emptyList()
) {
    fun register(dispatcher: CommandDispatcher<FabricClientCommandSource>) {
        val command = ClientCommandManager.literal(name)
            .executes { context -> execute(context) }

        buildCommand(command)
        dispatcher.register(command)

        aliases.forEach { alias ->
            val aliasCommand = ClientCommandManager.literal(alias)
                .executes { context -> execute(context) }

            buildCommand(aliasCommand)
            dispatcher.register(aliasCommand)
        }
    }

    abstract fun execute(context: CommandContext<FabricClientCommandSource>): Int

    open fun buildCommand(builder: LiteralArgumentBuilder<FabricClientCommandSource>) {}
}