package co.stellarskys.stellaKsp

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration

class FeatureProcessor(private val codeGenerator: CodeGenerator, private val projectName: String) : SymbolProcessor {
    private var invoked = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (invoked) return emptyList()

        val modules = resolver.getSymbolsWithAnnotation("co.stellarskys.stella.annotations.Module")
        val commands = resolver.getSymbolsWithAnnotation("co.stellarskys.stella.annotations.Command")

        val moduleDecls = modules.filterIsInstance<KSClassDeclaration>().toList()
        val commandDecls = commands.filterIsInstance<KSClassDeclaration>().toList()

        if (moduleDecls.isEmpty() && commandDecls.isEmpty()) return emptyList()

        val cleanProjectName = projectName.split("-", "_", " ").joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
        val className = "${cleanProjectName}ModuleProvider"

        generateRegistry(moduleDecls, commandDecls, className)
        invoked = true

        return emptyList()
    }

    private fun generateRegistry(moduleDecls: List<KSClassDeclaration>, commandDecls: List<KSClassDeclaration>, className: String) {
        val sourceFiles = (moduleDecls + commandDecls)
            .mapNotNull { it.containingFile }
            .distinct()
            .toTypedArray()

        val deps = if (sourceFiles.isEmpty()) Dependencies.ALL_FILES else Dependencies(true, *sourceFiles)

        val file = codeGenerator.createNewFile(deps, "co.stellarskys.stella.generated", className)
        file.writer().use { out ->
            out.appendLine("package co.stellarskys.stella.generated")
            out.appendLine()
            out.appendLine("import co.stellarskys.stella.managers.ModuleProvider")
            out.appendLine("import co.stellarskys.stella.api.handlers.Atlas")
            out.appendLine()
            out.appendLine("class $className : ModuleProvider {")
            out.appendLine("  override val modules: List<Class<*>> = listOf(")
            moduleDecls.forEach { decl ->
                out.appendLine("    ${decl.qualifiedName!!.asString()}::class.java,")
            }
            out.appendLine("  )")
            out.appendLine("  override val commands: List<Atlas> = listOf(")
            commandDecls.forEach { decl ->
                out.appendLine("    ${decl.qualifiedName!!.asString()},")
            }
            out.appendLine("  )")
            out.appendLine("}")
        }

        val spiFile = codeGenerator.createNewFile(
            dependencies = deps,
            packageName = "",
            fileName = "META-INF/services/co.stellarskys.stella.managers.ModuleProvider",
            extensionName = ""
        )
        spiFile.writer().use { out ->
            out.write("co.stellarskys.stella.generated.$className\n")
        }
    }
}
