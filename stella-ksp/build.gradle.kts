plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ksp)
    `maven-publish`
}

repositories {
    mavenCentral()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "stella-ksp"
            from(components["java"])
        }
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.ksp.api)
    implementation("com.google.auto.service:auto-service-annotations:1.1.1")
    ksp("dev.zacsweers.autoservice:auto-service-ksp:1.2.0")
}