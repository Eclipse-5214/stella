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
    inline fun <reified T> runs(noinline block: (T) -> Unit) = root.runs(block)
    inline fun <reified T, reified U> runs(noinline block: (T, U) -> Unit) = root.runs(block)
    fun literal(string: String, block: LiteralNode.() -> Unit = {}): LiteralNode = root.literal(string, block)
    fun literal(vararg names: String, block: LiteralNode.() -> Unit = {}): LiteralNode = root.literal(*names, block = block)
    fun executable(block: Executable.() -> Unit): Executable = root.executable(block)

    fun register(dispatcher: CommandDispatcher<*>) {
        Commodore(root).register(dispatcher)
    }


    typealias Greedy = GreedyString
}