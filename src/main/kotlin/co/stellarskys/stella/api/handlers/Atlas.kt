package co.stellarskys.stella.api.handlers

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.*
import com.mojang.brigadier.builder.ArgumentBuilder
import com.mojang.brigadier.builder.LiteralArgumentBuilder
import com.mojang.brigadier.builder.RequiredArgumentBuilder
import com.mojang.brigadier.context.CommandContext
import kotlin.reflect.KProperty

/*
 * Modified version of commodore that works as an object :D
 * https://github.com/Stivais/Commodore/
 */
@Suppress("UNCHECKED_CAST", "UNUSED")
open class Atlas(val name: String, vararg val aliases: String) {
    val builder: LiteralArgumentBuilder<Any?> = LiteralArgumentBuilder.literal(name)
    val pendingArgs = mutableListOf<ArgumentData<*>>()
    val arg = ArgBuilder()

    class ArgumentData<T>(var argName: String, val type: ArgumentType<T>, val suggestions: List<String>? = null, var isNullable: Boolean = false) {
        fun optional(): ArgumentData<T?> {
            this.isNullable = true
            return this as ArgumentData<T?>
        }

        operator fun provideDelegate(thisRef: Any?, property: KProperty<*>): ArgumentData<T> {
            if (argName.isEmpty()) argName = property.name
            return this
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
            val ctx = context.get() ?: error("Command accessed outside of execution")
            return try { ctx.getArgument(argName, Any::class.java) as T }
            catch (e: IllegalArgumentException) { if (isNullable) null as T else throw e }
        }
    }

    inner class ArgBuilder() {
        fun string(name: String = "", vararg suggestions: String) = string(name, suggestions.toList())
        fun string(name: String = "", suggestions: List<String>? = null) = ArgumentData(name, StringArgumentType.word(), suggestions).also { pendingArgs.add(it) }
        fun greedy(name: String = "") = ArgumentData(name, StringArgumentType.greedyString()).also { pendingArgs.add(it) }
        fun int(name: String = "", min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE) = ArgumentData(name, IntegerArgumentType.integer(min, max)).also { pendingArgs.add(it) }
        fun bool(name: String = "") = ArgumentData(name, BoolArgumentType.bool()).also { pendingArgs.add(it) }
    }

    fun runs(block: () -> Unit) = buildChain(block)

    inline fun <reified T> runs(name: String? = null, crossinline block: (T) -> Unit) {
        val nodeName = name ?: "arg"

        val type = when (T::class) {
            String::class -> StringArgumentType.word()
            Greedy::class -> StringArgumentType.greedyString()
            else -> throw IllegalArgumentException("Unsupported type")
        }

        pendingArgs.add(ArgumentData(nodeName, type))
        if (null is T) builder.executes { block(null as T); 1 }

        buildChain {
            val raw = context.get()!!.getArgument(nodeName, Any::class.java)
            val value = if (T::class == Greedy::class) Greedy(raw as String) as T else raw as T
            block(value)
        }
    }

    fun buildChain(block: () -> Unit) {
        var tail: ArgumentBuilder<Any?, *>? = null
        for (i in pendingArgs.indices.reversed()) {
            val data = pendingArgs[i]
            val node = RequiredArgumentBuilder.argument<Any?, Any>(data.argName, data.type as ArgumentType<Any>)

            if (data.suggestions != null) node.suggests { _, b ->
                data.suggestions.forEach { b.suggest(it) }
                b.buildFuture()
            }

            if (tail == null) {
                node.executes { ctx ->
                    context.set(ctx)
                    try { block(); 1 } finally { context.remove() }
                }
            } else node.then(tail)
            tail = node
        }

        if (tail != null) {
            builder.then(tail)
            if (pendingArgs.firstOrNull()?.isNullable == true) builder.executes { ctx ->
                context.set(ctx)
                try { block(); 1 } finally { context.remove() }
            }
        } else builder.executes { ctx ->
            context.set(ctx)
            try { block(); 1 } finally { context.remove() }
        }

        pendingArgs.clear()
    }

    fun literal(name: String, block: Atlas.() -> Unit) {
        val child = Atlas(name)
        child.block()
        builder.then(child.builder)
    }

    fun register(dispatcher: CommandDispatcher<*>) {
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

    companion object { val context = ThreadLocal<CommandContext<Any?>>() }
    @JvmInline value class Greedy(val string: String)
}