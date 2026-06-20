pluginManagement {
    repositories {
        mavenLocal()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.10-alpha.2"
}

stonecutter {
    create(rootProject) {
        versions("26.1" /*, "26.2"*/)
        vcsVersion = "26.1"
    }
}

rootProject.name = "Stella"
include("stella-ksp")