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
        val moduleDecls = modules.filterIsInstance<KSClassDeclaration>().toList()
        val commandDecls = commands.filterIsInstance<KSClassDeclaration>().toList()

        val sourceFiles = (moduleDecls + commandDecls)
            .mapNotNull { it.containingFile }
            .distinct()
            .toTypedArray()

        val deps = if (sourceFiles.isEmpty()) Dependencies.ALL_FILES else Dependencies(false, *sourceFiles)

        val file = codeGenerator.createNewFile(
            deps,
            "co.stellarskys.stella.generated",
            "GeneratedFeatureRegistry"
        )

        file.writer().use { out ->
            out.appendLine("package co.stellarskys.stella.generated")
            out.appendLine()
            out.appendLine("import co.stellarskys.stella.api.handlers.Atlas")
            out.appendLine()
            out.appendLine("object GeneratedFeatureRegistry {")
            out.appendLine("  val modules: List<Class<*>> = listOf(")

            moduleDecls.forEach { decl ->
                out.appendLine("    ${decl.qualifiedName!!.asString()}::class.java,")
            }
            out.appendLine("  )")
            out.appendLine("  val commands: List<Atlas> = listOf(")

            commandDecls.forEach { decl ->
                out.appendLine("    ${decl.qualifiedName!!.asString()},")
            }

            out.appendLine("  )")
            out.appendLine("}")
        }
    }
}
