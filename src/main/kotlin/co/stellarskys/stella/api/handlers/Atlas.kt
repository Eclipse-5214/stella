package co.stellarskys.stella.api.handlers

import com.github.stivais.commodore.Commodore
import com.github.stivais.commodore.nodes.Executable
import com.github.stivais.commodore.nodes.LiteralNode
import com.github.stivais.commodore.utils.GreedyString
import com.mojang.brigadier.CommandDispatcher

/* Modified version of commodore that works as an object :D */
open class Atlas(val root: LiteralNode) {
    constructor(
        vararg name: String
    ) : this(LiteralNode(name[0], name.drop(1)))

    /**
     * DSL access to the root node for object-style definitions.
     */
    fun runs(block: () -> Unit) = root.runs(block)

    inline fun <reified T> runs(noinline block: (T) -> Unit) =
        runsInternal(T::class.java, block)

    inline fun <reified T, reified U> runs(noinline block: (T, U) -> Unit) =
        runsInternal(T::class.java, U::class.java, block)

    /**
     * These are the "Real" functions.
     * KSP can resolve these easily because they don't use 'reified'.
     */
    fun <T> runsInternal(clazz: Class<T>, block: (T) -> Unit) {
        root.runs(clazz, block)
    }

    fun <T, U> runsInternal(clazz1: Class<T>, clazz2: Class<U>, block: (T, U) -> Unit) {
        root.runs(clazz1, clazz2, block)
    }

    fun literal(string: String, block: LiteralNode.() -> Unit = {}): LiteralNode = root.literal(string, block)
    fun literal(vararg names: String, block: LiteralNode.() -> Unit = {}): LiteralNode = root.literal(*names, block = block)
    fun executable(block: Executable.() -> Unit): Executable = root.executable(block)

    fun register(dispatcher: CommandDispatcher<*>) {
        Commodore(root).register(dispatcher)
    }


    typealias Greedy = GreedyString
}