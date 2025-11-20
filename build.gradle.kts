import dev.deftu.gradle.utils.version.MinecraftVersions
import dev.deftu.gradle.utils.includeOrShade

plugins {
    java
    kotlin("jvm")
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools")
    id("dev.deftu.gradle.tools.resources")
    id("dev.deftu.gradle.tools.bloom")
    id("dev.deftu.gradle.tools.shadow")
    id("dev.deftu.gradle.tools.minecraft.loom")
    id("dev.deftu.gradle.tools.minecraft.releases")
}

repositories {
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.hypixel.net/repository/Hypixel/")
    maven("https://api.modrinth.com/maven")
    maven("https://maven.teamresourceful.com/repository/maven-public/")
}

toolkitMultiversion {
    moveBuildsToRootProject.set(true)
}

toolkitLoomHelper {
    if (!mcData.isNeoForge) {
        useMixinRefMap(modData.id)
    }

    if (mcData.isForge) {
        useTweaker("org.spongepowered.asm.launch.MixinTweaker")
        useForgeMixin(modData.id)
    }

    if (mcData.isForgeLike && mcData.version >= MinecraftVersions.VERSION_1_16_5) {
        useKotlinForForge()
    }
}

loom {
    if (mcData.isFabric && mcData.version >= MinecraftVersions.VERSION_1_16_5) {
        accessWidenerPath.set(rootProject.file("src/main/resources/stella.accesswidener"))
    }
}

val clocheAction: Action<ExternalModuleDependency> = Action {
    attributes {
        attribute(Attribute.of("earth.terrarium.cloche.modLoader", String::class.java), "fabric")
        attribute(Attribute.of("earth.terrarium.cloche.minecraftVersion", String::class.java),
            when (mcData.version) {
                MinecraftVersions.VERSION_1_21_10 -> "1.21.9"
                else -> mcData.toString().substringBefore("-")
            }
        )
    }
}


dependencies {
    implementation(include("io.github.classgraph:classgraph:4.8.184")!!)
    modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")
    modImplementation(include("xyz.meowing:vexel-${mcData}:124")!!)
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
    modImplementation(include("net.hypixel:mod-api:1.0.1")!!)
    modImplementation(include("maven.modrinth:hypixel-mod-api:1.0.1+build.1+mc1.21")!!)
    modImplementation("me.owdding:item-data-fixer:1.0.5", clocheAction)
    modImplementation("tech.thatgravyboat:skyblock-api:3.0.23") {
        exclude("me.owdding")
        clocheAction.execute(this)
    }
    include("tech.thatgravyboat:skyblock-api:3.0.23", clocheAction)
}