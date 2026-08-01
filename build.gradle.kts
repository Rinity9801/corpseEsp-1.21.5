plugins {
    id("fabric-loom") version "1.16-SNAPSHOT"
    id("maven-publish")
    kotlin("jvm") version "2.4.0"
    id("dev.kikugie.stonecutter")
}

val fullVersion = stonecutter.current.version
val mc = fullVersion.substringBefore("-")
val is26_1_2 = mc == "26.1.2"
val isCheatVariant = fullVersion.endsWith("-cheat")

// The external ESP feed lives in local/ and must never reach a published jar. It's
// compiled in only when that directory exists AND this isn't a release build, so
// `-Prelease` produces a clean jar from a working tree that still has local/ in it.
val isRelease = providers.gradleProperty("release").isPresent
val includeLocalEsp = !isRelease && rootProject.file("local/esp/java").exists()

version = property("mod_version").toString()
group = property("maven_group").toString()

base {
    archivesName.set(property("archives_base_name").toString())
}

loom {
    if (is26_1_2) {
        noIntermediateMappings()
    }

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
        if (is26_1_2) {
            // client26 files are synced verbatim (no stonecutter preprocessing), so
            // cheat-only code lives in its own tree included only for -cheat.
            // local/esp/ is the external ESP feed: present on dev machines, never copied
            // into a release, so it compiles in here and is simply absent from published
            // jars. Guarded on existence rather than a flag — nothing to remember, and a
            // checkout without the directory still builds.
            java.setSrcDirs(buildList {
                add(rootProject.file("src/client26/java").path)
                if (isCheatVariant) add(rootProject.file("src/client26cheat/java").path)
                if (includeLocalEsp) add(rootProject.file("local/esp/java").path)
            })
            resources.srcDir(rootProject.file("src/client26/resources"))
            // 26.1.2 Kotlin (Vexel GUI screens) lives in its own tree — the shared
            // src/client/kotlin is Yarn-mapped and doesn't compile against 26.x.
            // The java dir is included so kotlinc can resolve the mod's Java classes
            // (joint compilation; javac still owns the .java files).
            kotlin.setSrcDirs(buildList {
                add(rootProject.file("src/client26/kotlin").path)
                add(rootProject.file("src/client26/java").path)
                if (includeLocalEsp) {
                    add(rootProject.file("local/esp/java").path)
                    add(rootProject.file("local/esp/kotlin").path)
                }
                if (isCheatVariant) {
                    add(rootProject.file("src/client26cheat/kotlin").path)
                    add(rootProject.file("src/client26cheat/java").path)
                }
            })
        } else {
            kotlin {
                srcDir("src/client/kotlin")
            }
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
    if (!is26_1_2) {
        add("mappings", "net.fabricmc:yarn:${property("yarn_mappings")}:v2")
        add("modImplementation", "net.fabricmc:fabric-loader:${(findProperty("loader_version") ?: "").toString()}")
        add("modImplementation", "net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
    } else {
        implementation("net.fabricmc:fabric-loader:${(findProperty("loader_version") ?: "").toString()}")
        implementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
        implementation("net.fabricmc.fabric-api:fabric-key-mapping-api-v1:2.0.4+e2bdee784c")
    }

    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    if (!is26_1_2) {
        add("modImplementation", "net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    } else {
        implementation("net.fabricmc:fabric-language-kotlin:${property("fabric_kotlin_version")}")
    }

    // Vexel GUI Library
    if (!is26_1_2) {
        add("modImplementation", include(property("vexel_dep").toString())!!)
    } else {
        implementation("xyz.meowing:knit-26.1.2-fabric:26.1.2-local")
        implementation(include("xyz.meowing:knit-26.1.2-fabric:26.1.2-local")!!)
        implementation(property("vexel_dep").toString())
        implementation(include(property("vexel_dep").toString())!!)
    }
}

// Stonecutter constants for preprocessor
stonecutter {
    constants["is1_21_11"] = eval(mc, ">=1.21.11")
    constants["is26_1_2"] = is26_1_2
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

val targetJavaVersion = if (is26_1_2) 25 else 21
if (is26_1_2) {
    val sync26ClientSources = tasks.register<Sync>("sync26ClientSources") {
        dependsOn("stonecutterGenerateClient")
        from(rootProject.file("src/client26/java"))
        if (isCheatVariant) from(rootProject.file("src/client26cheat/java"))
        into(layout.buildDirectory.dir("generated/stonecutter/client/java"))
    }
    val sync26ClientResources = tasks.register<Sync>("sync26ClientResources") {
        dependsOn("stonecutterGenerateClient")
        from(rootProject.file("src/client26/resources/miningqol.client.mixins.json"))
        into(layout.buildDirectory.dir("generated/stonecutter/client/resources"))
    }
    tasks.named("compileClientJava") {
        dependsOn(sync26ClientSources)
    }
    tasks.named<ProcessResources>("processClientResources") {
        dependsOn(sync26ClientResources)
        duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        // The 26 source tree replaces src/client, which orphaned the shared assets
        // (textures/lang). Pull just the assets back in.
        from(rootProject.file("src/client/resources")) {
            include("assets/**")
        }
    }
    tasks.withType<Jar>().configureEach {
        if (name == "sourcesJar") {
            dependsOn(sync26ClientResources)
        }
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(targetJavaVersion.toString()))
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

if (is26_1_2) {
    tasks.withType<Jar>().configureEach {
        if (name == "sourcesJar") {
            duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
        }
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
