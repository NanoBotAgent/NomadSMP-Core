plugins {
    java
    id("io.github.goooler.shadow") version "8.1.8"
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
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://maven.enginehub.org/repo/")
    flatDir { dirs("libs") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.53-stable")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.4.2")
    compileOnly(":worldedit-bukkit-7.4.3-beta-01")
}

tasks.shadowJar {
    archiveClassifier.set("")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
