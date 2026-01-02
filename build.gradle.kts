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

    strictMaven("https://jitpack.io", "com.github.stivais")
    strictMaven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1", "me.djtheredstoner")
    strictMaven("https://repo.hypixel.net/repository/Hypixel", "net.hypixel")
    strictMaven("https://api.modrinth.com/maven", "maven.modrinth")
    strictMaven("https://maven.teamresourceful.com/repository/maven-public/", "tech.thatgravyboat", "com.terraformersmc", "me.owdding")
    strictMaven("https://maven.deftu.dev/snapshots", "dev.deftu")
    strictMaven("https://maven.deftu.dev/releases", "dev.deftu")
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings(loom.officialMojangMappings())

    ksp(project(":stella-ksp"))

    modRuntimeOnly(libs.devauth)

    modImplementation("fabric-api".mc(mc))
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.language.kotlin)
    modImplementation(libs.hypixel.modapi)
    modImplementation(libs.hypixel.modapi.fabric)

    shadow("omnicore".mc(mc))
    shadow("textile".mc(mc))
    shadow("vexel".mc(mc))
    shadow(libs.classgraph)
    shadow(libs.commodore)

    modImplementation(libs.skyblock.api) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$mc") }
    }

    include(libs.skyblock.api) {
        capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$mc-remapped") }
    }
}

loom {
    fabricModJsonPath = rootProject.file("src/main/resources/fabric.mod.json") // Useful for interface injection
    //accessWidenerPath = rootProject.file("src/main/resources/template.accesswidener")

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

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}

sourceSets.main { java.srcDir("build/generated/ksp/main/kotlin") }

fun String.mc(mc: String): Provider<MinimalExternalModuleDependency> = project.extensions.getByType<VersionCatalogsExtension>().named("libs").findLibrary("$this-${mc.replace(".", "_")}").get()

fun DependencyHandler.shadow(dep: Any) {
    include(dep)
    modImplementation(dep)
}