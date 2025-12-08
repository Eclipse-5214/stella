package co.stellarskys.stella.utils

import com.github.stivais.commodore.nodes.Executable
import com.github.stivais.commodore.nodes.LiteralNode
import com.mojang.brigadier.CommandDispatcher

/* Modified version of commodor that works as an object :D */
open class Commodore(val root: LiteralNode) {
    constructor(
        vararg name: String
    ) : this(LiteralNode(name[0], name.drop(1)))

    /**
     * DSL access to the root node for object-style definitions.
     */
    fun runs(function: Function<Unit>) = root.runs(function)

    fun runs(block: () -> Unit) = root.runs(block)

    fun literal(string: String, block: LiteralNode.() -> Unit = {}): LiteralNode {
        return root.literal(string, block)
    }

    fun literal(vararg names: String, block: LiteralNode.() -> Unit = {}): LiteralNode {
        return root.literal(*names, block = block)
    }

    fun executable(block: Executable.() -> Unit): Executable {
        return root.executable(block)
    }

    fun register(dispatcher: CommandDispatcher<*>) {
        val cmd = com.github.stivais.commodore.Commodore(root)
        cmd.register(dispatcher)
    }
}