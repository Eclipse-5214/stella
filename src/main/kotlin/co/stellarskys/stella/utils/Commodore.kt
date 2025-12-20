package co.stellarskys.stella.utils

import com.github.stivais.commodore.nodes.Executable
import com.github.stivais.commodore.nodes.LiteralNode
import com.mojang.brigadier.CommandDispatcher

/* Modified version of commodore that works as an object :D */
open class Commodore(val root: LiteralNode) {
    constructor(
        vararg name: String
    ) : this(LiteralNode(name[0], name.drop(1)))

    /**
     * DSL access to the root node for object-style definitions.
     */
    fun runs(block: () -> Unit) = root.runs(block)
    fun literal(string: String, block: LiteralNode.() -> Unit = {}): LiteralNode = root.literal(string, block)
    fun literal(vararg names: String, block: LiteralNode.() -> Unit = {}): LiteralNode =
        root.literal(*names, block = block)

    fun executable(block: Executable.() -> Unit): Executable = root.executable(block)

    fun register(dispatcher: CommandDispatcher<*>) {
        com.github.stivais.commodore.Commodore(root).register(dispatcher)
    }
}