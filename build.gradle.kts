plugins {
    kotlin("jvm")
    id("fabric-loom")
}

val minecraft = stonecutter.current.version
val loader = "fabric"

version = "${property("mod.version")}+${minecraft}"
base.archivesName = property("mod.id") as String

val requiredJava = when {
    stonecutter.eval(minecraft, ">=1.20.6") -> JavaVersion.VERSION_21
    stonecutter.eval(minecraft, ">=1.18") -> JavaVersion.VERSION_17
    stonecutter.eval(minecraft, ">=1.17") -> JavaVersion.VERSION_16
    else -> JavaVersion.VERSION_1_8
}

repositories {
    fun strictMaven(url: String, alias: String, vararg groups: String) = exclusiveContent {
        forRepository { maven(url) { name = alias } }
        filter { groups.forEach(::includeGroup) }
    }
    strictMaven("https://www.cursemaven.com", "CurseForge", "curse.maven")
    strictMaven("https://api.modrinth.com/maven", "Modrinth", "maven.modrinth")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://api.modrinth.com/maven")
    maven("https://jitpack.io")
    maven("https://maven.teamresourceful.com/repository/maven-public/")
    maven("https://maven.deftu.dev/releases")
    maven("https://maven.deftu.dev/snapshots")
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraft")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${property("deps.fabric_loader")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("deps.fabric_api")}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("deps.fabric-kotlin")}")
    with(libs.textile.get()) { modImplementation(include("${this.group}:${this.name}-$minecraft-$loader:${this.version}")!!) }
    with(libs.omnicore.get()) { modImplementation(include("${this.group}:${this.name}-$minecraft-$loader:${this.version}")!!) }
    implementation(include("io.github.classgraph:classgraph:4.8.184")!!)
    modImplementation(include("co.stellarskys:vexel-$minecraft-$loader:130")!!)
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")

    property("skyblock_api_version").let {
        api("tech.thatgravyboat:skyblock-api:$it") {
            capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$minecraft") }
        }
        include("tech.thatgravyboat:skyblock-api:$it") {
            capabilities { requireCapability("tech.thatgravyboat:skyblock-api-$minecraft-remapped") }
        }
    }

    property("hypixel_api_version").let {
        modImplementation(include("net.hypixel:mod-api:$it")!!)
        modImplementation(include("maven.modrinth:hypixel-mod-api:$it+build.1+mc1.21")!!)
    }

    property("commodore_version").let {
        implementation("com.github.stivais:Commodore:$it")
        include("com.github.stivais:Commodore:$it")
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

java {
    withSourcesJar()
    targetCompatibility = requiredJava
    sourceCompatibility = requiredJava
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

        val mixinJava = "JAVA_${requiredJava.majorVersion}"
        filesMatching("*.mixins.json") { expand("java" to mixinJava) }
    }

    // Builds the version into a shared folder in `build/libs/${mod version}/`
    register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.map { it.archiveFile }, remapSourcesJar.map { it.archiveFile })
        into(rootProject.layout.buildDirectory.file("libs/${project.property("mod.version")}"))
        dependsOn("build")
    }
}