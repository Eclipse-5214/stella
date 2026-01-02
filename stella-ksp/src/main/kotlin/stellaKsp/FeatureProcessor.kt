package co.stellarskys.stellaKsp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class FeatureProcessor(
    private val codeGenerator: CodeGenerator
) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()

        val modules = resolver.getSymbolsWithAnnotation("co.stellarskys.stella.annotations.Module")
        val commands = resolver.getSymbolsWithAnnotation("co.stellarskys.stella.annotations.Command")

        generateRegistry(modules, commands)
        invoked = true

        return emptyList()
    }

    private fun generateRegistry(
        modules: Sequence<KSAnnotated>,
        commands: Sequence<KSAnnotated>
    ) {
        val file = codeGenerator.createNewFile(
            Dependencies.ALL_FILES,
            "co.stellarskys.stella.generated",
            "GeneratedFeatureRegistry"
        )

        file.writer().use { out ->
            out.appendLine("package co.stellarskys.stella.generated")
            out.appendLine()
            out.appendLine("import co.stellarskys.stella.utils.Commodore")
            out.appendLine()
            out.appendLine("object GeneratedFeatureRegistry {")
            out.appendLine("  val modules: List<Class<*>> = listOf(")

            modules.forEach { sym ->
                val name = (sym as KSClassDeclaration).qualifiedName!!.asString()
                out.appendLine("    $name::class.java,")
            }
            out.appendLine("  )")
            out.appendLine("  val commands: List<Commodore> = listOf(")

            commands.forEach { sym ->
                val name = (sym as KSClassDeclaration).qualifiedName!!.asString()
                out.appendLine("    $name,")
            }

            out.appendLine("  )")
            out.appendLine("}")
        }
    }
}
