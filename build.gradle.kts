import org.jetbrains.kotlin.gradle.dsl.JvmTarget

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
    maven("https://jitpack.io")
    maven("https://maven.teamresourceful.com/repository/maven-public/")
}

toolkitMultiversion {
    moveBuildsToRootProject.set(true)
}

dependencies {
    with(libs.textile.get()) { modImplementation(include("${this.group}:${this.name}-$mcData:${this.version}")!!) }
    with(libs.omnicore.get()) { modImplementation(include("${this.group}:${this.name}-$mcData:${this.version}")!!) }
    implementation(include("io.github.classgraph:classgraph:4.8.184")!!)
    modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
    modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")
    modImplementation(include("co.stellarskys:vexel-${mcData}:139")!!)
    runtimeOnly("me.djtheredstoner:DevAuth-fabric:1.2.1")

    property("skyblock_api_version").let {
        api("tech.thatgravyboat:skyblock-api:$it") {
            capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${mcData.version}") }
        }
        include("tech.thatgravyboat:skyblock-api:$it") {
            capabilities { requireCapability("tech.thatgravyboat:skyblock-api-${mcData.version}-remapped") }
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

/* thanks odin! */
tasks {
    compileKotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_21
            freeCompilerArgs.add("-Xlambdas=class") //Commodore
        }
    }
}