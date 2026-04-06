pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
        maven("https://maven.kikugie.dev/snapshots")
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9"
}

stonecutter {
    create(rootProject) {
        versions("1.21.10-legit", "1.21.10-cheat", "1.21.11-legit", "1.21.11-cheat")
        vcsVersion = "1.21.11-cheat"
    }
}
