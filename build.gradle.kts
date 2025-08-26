import dev.deftu.gradle.utils.MinecraftVersion
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

    if (mcData.isForgeLike && mcData.version >= MinecraftVersion.VERSION_1_16_5) {
        useKotlinForForge()
    }
}

dependencies {
    val textileVersion = "0.3.1"
    val omnicoreVersion = "0.6.0"
    modImplementation("dev.deftu:textile-$mcData:$textileVersion")
    modImplementation("dev.deftu:omnicore-$mcData:$omnicoreVersion")

    if (mcData.isFabric) {
        modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
        modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")
    } else if (mcData.version <= MinecraftVersion.VERSION_1_12_2) {
        implementation(includeOrShade(kotlin("stdlib-jdk8"))!!)
        implementation(includeOrShade("org.jetbrains.kotlin:kotlin-reflect:1.6.10")!!)

        modImplementation(includeOrShade("org.spongepowered:mixin:0.7.11-SNAPSHOT")!!)

        includeOrShade("dev.deftu:textile-$mcData:$textileVersion")
        includeOrShade("dev.deftu:omnicore-$mcData:$omnicoreVersion")
    }
}

tasks {

    fatJar {
        if (mcData.isLegacyForge) {
            relocate("dev.deftu.textile", "dev.deftu.favorita.textile")
            relocate("dev.deftu.omnicore", "dev.deftu.favorita.omnicore")
        }
    }

}
