plugins {
    java
    id("com.gradleup.shadow") version "9.0.0-beta4"
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
    maven("https://ci.enginehub.org/repo/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.sk89q.worldedit:worldedit-core:7.3.0")
    compileOnly("com.sk89q.worldedit:worldedit-bukkit:7.3.0")
}

tasks.shadowJar {
    archiveClassifier.set("")
    minimize()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
