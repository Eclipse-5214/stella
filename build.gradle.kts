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

dependencies {
    modImplementation(includeOrShade("org.reflections:reflections:0.10.2")!!)
    modImplementation(includeOrShade("org.javassist:javassist:3.30.2-GA")!!)
    modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")
    modImplementation(includeOrShade("gg.essential:elementa:710")!!)
    modImplementation(includeOrShade("gg.essential:universalcraft-${mcData}:436")!!)
    modImplementation(include("xyz.meowing:vexel-${mcData}:106")!!)

    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")
}

tasks {
    fatJar {
        if (mcData.isLegacyForge) {
            relocate("gg.essential.elementa", "co.stellarskys.elementa")
            relocate("gg.essential.universalcraft", "co.stellarskys.universalcraft")
        }
    }

}