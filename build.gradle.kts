plugins {
    id("fabric-loom") version "1.15.5"
    id("maven-publish")
    kotlin("jvm") version "2.1.21"
    id("dev.kikugie.stonecutter")
}

val fullVersion = stonecutter.current.version
val mc = fullVersion.substringBefore("-")

version = property("mod_version").toString()
group = property("maven_group").toString()

base {
    archivesName.set(property("archives_base_name").toString())
}

loom {
    splitEnvironmentSourceSets()

    mods {
        create("miningqol") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

sourceSets {
    main {
        kotlin {
            srcDir("src/main/kotlin")
        }
    }
    named("client") {
        kotlin {
            srcDir("src/client/kotlin")
        }
    }
}

fabricApi {
    configureDataGeneration {
        client = true
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://maven.deftu.dev/snapshots")
    maven("https://maven.deftu.dev/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:${(findProperty("minecraft_version") ?: mc).toString()}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${(findProperty("loader_version") ?: "").toString()}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    modImplementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")

    // Vexel GUI Library
    modImplementation(include(property("vexel_dep").toString())!!)
}

// Stonecutter constants for preprocessor
stonecutter {
    constants["is1_21_11"] = eval(mc, ">=1.21.11")
    constants["isCheat"] = fullVersion.endsWith("-cheat")
}

tasks.processResources {
    inputs.property("version", project.version)
    inputs.property("minecraft_version", (findProperty("minecraft_version") ?: mc).toString())
    inputs.property("loader_version", (findProperty("loader_version") ?: "").toString())
    inputs.property("fabric_kotlin_version", (findProperty("fabric_kotlin_version") ?: "").toString())
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to project.version,
            "minecraft_version" to (findProperty("minecraft_version") ?: mc).toString(),
            "loader_version" to (findProperty("loader_version") ?: "").toString(),
            "fabric_kotlin_version" to (findProperty("fabric_kotlin_version") ?: "").toString()
        )
    }
}

val targetJavaVersion = 21
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion.set(JavaLanguageVersion.of(targetJavaVersion))
    }
    withSourcesJar()
}

tasks.jar {
    from("LICENSE") {
        rename { "${it}_${project.property("archives_base_name")}" }
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = property("archives_base_name").toString()
            from(components["java"])
        }
    }
    repositories {}
}
