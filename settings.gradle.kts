pluginManagement {
    repositories {
        maven { url = uri("https://maven.leclowndu93150.dev/releases") }
        gradlePluginPortal()
        mavenCentral()
        maven { url = uri("https://maven.fabricmc.net/") }
        maven { url = uri("https://maven.neoforged.net/releases") }
        maven { url = uri("https://repo.spongepowered.org/repository/maven-public/") }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
    id("org.jetbrains.kotlin.jvm") version "2.1.20" apply false
    id("dev.prism.settings") version "+"
}

rootProject.name = "Essential Patcher"

prism {
    version("1.21.1") {
        common()
        fabric()
        neoforge()
    }
    version("1.20.1") {
        common()
        fabric()
        forge()
    }
}
