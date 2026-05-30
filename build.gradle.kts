import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.Instant

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.loom)
    alias(libs.plugins.ksp)
}

val mc = stonecutter.current.version
val loader = "fabric"

version = "${property("mod.version")}+${mc}"
base.archivesName = property("mod.id") as String

repositories {
    @Suppress("UnstableApiUsage")
    fun strictMaven(url: String, vararg groups: String) = maven(url) { content { groups.forEach(::includeGroupAndSubgroups) } }

    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    strictMaven("https://repo.hypixel.net/repository/Hypixel", "net.hypixel")
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
    strictMaven("https://maven.teamresourceful.com/repository/maven-public/", "tech.thatgravyboat", "com.terraformersmc", "me.owdding")
    strictMaven("https://maven.cassian.cc", "cc.cassian")
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    ksp(project(":stella-ksp"))
    runtimeOnly(libs.devauth)

    implementation("fabric-api".mc(mc))
    implementation(libs.fabric.loader)
    implementation(libs.fabric.language.kotlin)
    implementation(libs.hypixel.modapi)
    implementation(libs.hypixel.modapi.fabric)

    api(libs.skyblock.api) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$mc") }
    }

    include(libs.skyblock.api) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$mc") }
    }

    shadow(libs.lwjgl.nanovg)
    listOf("windows", "linux", "linux-arm64", "macos", "macos-arm64").forEach { os ->
        shadow("${libs.lwjgl.nanovg.get()}:natives-$os")
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    accessWidenerPath = rootProject.file("src/main/resources/stella.classtweaker")

    decompilerOptions.named("vineflower") {
        options.put("mark-corresponding-synthetics", "1") // Adds names to lambdas - useful for mixins
    }

    runConfigs.all {
        ideConfigGenerated(true)
        vmArgs("-Dmixin.debug.export=true") // Exports transformed classes for debugging
        runDir = "../../run" // Shares the run directory between versions
    }
}

tasks {
    processResources {
        inputs.property("id", project.property("mod.id"))
        inputs.property("name", project.property("mod.name"))
        inputs.property("version", project.property("mod.version"))
        inputs.property("minecraft", project.property("mod.mc_dep"))

        val props = mapOf(
            "id" to project.property("mod.id"),
            "name" to project.property("mod.name"),
            "version" to project.property("mod.version"),
            "minecraft" to project.property("mod.mc_dep")
        )

        filesMatching("fabric.mod.json") { expand(props) }
    }

    val generateBuildInfo by registering {
        group = "build"
        description = "Generates the dynamic BuildInfo metadata source file."

        val targetDir = layout.buildDirectory.dir("generated/sources/buildinfo/kotlin/co/stellarskys/stella/api/update")
        outputs.dir(targetDir)

        doLast {
            val buildVersion = project.property("mod.version") as String
            val buildTime = Instant.now().toString()

            val outputFile = targetDir.get().file("BuildInfo.kt").asFile
            outputFile.parentFile.mkdirs()
            outputFile.writeText("""
                package co.stellarskys.stella.api.update

                object BuildInfo {
                    const val VERSION = "$buildVersion"
                    const val BUILD_TIMESTAMP = "$buildTime"
                }
            """.trimIndent())
        }
    }

    withType<KotlinCompile>().configureEach {
        dependsOn(generateBuildInfo)
    }

    matching { it.name.startsWith("kspKotlin") }.configureEach {
        dependsOn(generateBuildInfo)
    }

    register<Copy>("buildAndCollect") {
        group = "build"
        from(jar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }

}

kotlin {
    sourceSets.main {
        kotlin.srcDir(layout.buildDirectory.dir("generated/sources/buildinfo/kotlin"))
    }
}

fun String.mc(mc: String): Provider<MinimalExternalModuleDependency> = project.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${mc.replace(".", "_")}").get()
fun DependencyHandler.shadow(dep: Any) { include(dep); implementation(dep) }