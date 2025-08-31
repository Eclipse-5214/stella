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

dependencies {
    modImplementation(includeOrShade("org.reflections:reflections:0.10.2")!!)
    modImplementation(includeOrShade("org.javassist:javassist:3.30.2-GA")!!)
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")

    if (mcData.isFabric) {
        modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
        modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")

        if (mcData.version >= MinecraftVersions.VERSION_1_21_5) {
            modImplementation(includeOrShade("gg.essential:elementa:710")!!)
            modImplementation(includeOrShade("gg.essential:universalcraft-${mcData}:427")!!)
        }

    } else if (mcData.version <= MinecraftVersions.VERSION_1_12_2) {
        implementation(includeOrShade(kotlin("stdlib-jdk8"))!!)
        implementation(includeOrShade("org.jetbrains.kotlin:kotlin-reflect:1.6.10")!!)
        implementation(includeOrShade("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.1")!!)
        implementation(includeOrShade("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.2")!!)

        modImplementation(includeOrShade("org.spongepowered:mixin:0.7.11-SNAPSHOT")!!)
        modImplementation(includeOrShade("gg.essential:elementa:710")!!)
        modImplementation(includeOrShade("gg.essential:universalcraft-${mcData}:427")!!)

        runtimeOnly("me.djtheredstoner:DevAuth-forge-legacy:1.2.1")
    }
}

tasks {
    fatJar {
        if (mcData.isLegacyForge) {
            relocate("gg.essential.elementa", "co.stellarskys.elementa")
            relocate("gg.essential.universalcraft", "co.stellarskys.universalcraft")
        }
    }

}