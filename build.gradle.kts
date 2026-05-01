plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta17"
}

group = "com.nomadsmp"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "PaperMC"
    }
    maven("https://mvn.intellectualsites.com/content/repositories/snapshots/") {
        name = "IntellectualSites"
    }
    maven("https://maven.enginehub.org/repo/") {
        name = "EngineHub"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.53-stable")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.15.0")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.4.2")
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveClassifier = ""
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to version)
    }
}
