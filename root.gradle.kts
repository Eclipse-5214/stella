plugins {
    id("dev.deftu.gradle.multiversion-root")
}

preprocess {
    "1.21.10-fabric"(1_21_10, "srg") {
        "1.21.8-fabric"(1_21_08, "srg")
    }
}
