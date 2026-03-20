package co.stellarskys.stella.api.handlers

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder

/*
 * Modified version of commodore that works as an object :D
 * https://github.com/Stivais/Commodore/
 */
open class Atlas(val name: String, vararg val aliases: String) {
    val builder: LiteralArgumentBuilder<Any?> = LiteralArgumentBuilder.literal(name)

    fun runs(block: () -> Unit) { builder.executes { block(); 1} }
    inline fun <reified T> runs(crossinline block: (T) -> Unit) {
        val type = when (T::class) {
            String::class -> StringArgumentType.word()
            Greedy::class -> StringArgumentType.greedyString()
            else -> throw IllegalArgumentException("Unsupported type")
        }

        val argument = RequiredArgumentBuilder.argument<Any?, String>("arg", type)
            .executes { context ->
                val input = StringArgumentType.getString(context, "arg")
                val value = if (T::class == Greedy::class) Greedy(input) as T else input as T
                block(value); 1
            }

        builder.then(argument)
        if (null is T) builder.executes { block(null as T); 1 }
    }

    fun literal(name: String, block: Atlas.() -> Unit) {
        val child = Atlas(name)
        child.block()
        builder.then(child.builder)
    }

    fun register(dispatcher: CommandDispatcher<*>) {
        @Suppress("UNCHECKED_CAST")
        dispatcher as CommandDispatcher<Any>
        val root = builder.build()
        dispatcher.register(builder)
        for (alias in aliases) {
            val aliasBuilder = LiteralArgumentBuilder.literal<Any?>(alias)
            if (root.command != null) aliasBuilder.executes(root.command)
            aliasBuilder.redirect(root)
            dispatcher.register(aliasBuilder)
        }
    }

    @JvmInline value class Greedy(val string: String)
}