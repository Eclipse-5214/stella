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
    id("dev.kikugie.stonecutter") version "0.9-beta.3"
}

stonecutter {
    create(rootProject) {
        versions("26.1")
        vcsVersion = "26.1"
    }
}

rootProject.name = "Stella"
include("stella-ksp")