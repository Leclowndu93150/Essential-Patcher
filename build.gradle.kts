plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.8"

prism {
    curseMaven()

    metadata {
        modId = "essentialpatcher"
        name = "Essential Patcher"
        description = "Debloats and patches the Essential mod. Unlock cosmetics client-side, disable telemetry, remove ads, and more."
        license = "MIT"
    }

    publishing {

        changelog = "added Mod Menu integration on Fabric, added 1.19.2, fixed Skip Community Rules breaking friend requests, fixed the Modrinth install 404, cosmetic sync now reconnects on its own, updated to Essential 1.4.1.1"
        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1555663"
        }

        modrinth {
            accessToken = providers.environmentVariable("MODRINTH_TOKEN")
            projectId = "1aEzsYFA"
        }

        dependencies {
            curseforge {
                requires("essential-mod")
            }
            modrinth {
                requires("essential")
            }
        }
    }

    version("1.21.1") {
        publishingDependencies {
            requires("yacl")
        }
        common {
            localJar("libs/essential.jar")
            modCompileOnly("curse.maven:yacl-667299:7437845")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.18.6"
            fabricApi("0.116.10+1.21.1")
            mixins {
                disableAutoDetect()
            }
            dependencies {
                localJar("libs/essential.jar")
                modImplementation("curse.maven:yacl-667299:7437855")
                modCompileOnly("curse.maven:modmenu-308702:7808443")
            }
        }
        neoforge {
            loaderVersion = "21.1.223"
            loaderVersionRange = "[4,)"
            dependencies {
                localJar("libs/essential.jar")
                implementation("curse.maven:yacl-667299:7437845")
                runtimeOnly("curse.maven:essential-mod-546670:7806141")
            }
        }
    }

    version("1.21.2") {
        publishingDependencies {
            requires("yacl")
        }
        common {
            localJar("libs/essential.jar")
            modCompileOnly("curse.maven:yacl-667299:7437845")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.18.6"
            fabricApi("0.106.1+1.21.2")
            mixins {
                disableAutoDetect()
            }
            dependencies {
                localJar("libs/essential.jar")
                modImplementation("curse.maven:yacl-667299:7437849")
                modCompileOnly("curse.maven:modmenu-308702:7808559")
            }
        }
    }

    version("1.21.9") {
        publishingDependencies {
            requires("yacl")
        }
        common {
            localJar("libs/essential-1.21.11.jar")
            modCompileOnly("curse.maven:yacl-667299:7437853")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.18.6"
            fabricApi("0.134.1+1.21.9")
            mixins {
                disableAutoDetect()
            }
            dependencies {
                localJar("libs/essential-1.21.11.jar")
                modImplementation("curse.maven:yacl-667299:7437852")
                modCompileOnly("curse.maven:modmenu-308702:7808781")
            }
        }
    }

    version("1.21.11") {
        publishingDependencies {
            requires("yacl")
        }
        common {
            localJar("libs/essential-1.21.11.jar")
            modCompileOnly("curse.maven:yacl-667299:7437853")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.18.6"
            fabricApi("0.141.4+1.21.11")
            mixins {
                disableAutoDetect()
            }
            dependencies {
                localJar("libs/essential-1.21.11.jar")
                modImplementation("curse.maven:yacl-667299:7437843")
                modCompileOnly("curse.maven:modmenu-308702:7808841")
            }
        }
    }

    version("26.1.2") {
        minecraftVersions("26.1.2", "26.2")
        publishingDependencies {
            requires("yacl")
        }
        common {
            localJar("libs/essential-26.1.2.jar")
            modCompileOnly("curse.maven:yacl-667299:7904436")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.18.6"
            fabricApi("0.149.1+26.1.2")
            mixins {
                disableAutoDetect()
            }
            dependencies {
                localJar("libs/essential-26.1.2.jar")
                modImplementation("curse.maven:yacl-667299:7904436")
                modCompileOnly("curse.maven:modmenu-308702:8065321")
            }
        }
    }

    version("1.19.2") {
        common {
            localJar("libs/essential-1.19.2.jar")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.16.14"
            fabricApi("0.77.0+1.19.2")
            mixins {
                disableAutoDetect()
            }
            dependencies {
                localJar("libs/essential-1.19.2.jar")
                modCompileOnly("curse.maven:modmenu-308702:4145207")
            }
        }
        forge {
            loaderVersion = "43.5.0"
            dependencies {
                localJar("libs/essential-1.19.2.jar")
            }
        }
    }

    version("1.20.1") {
        publishingDependencies {
            requires("yacl")
        }
        common {
            localJar("libs/essential-1.20.1.jar")
            modCompileOnly("curse.maven:yacl-667299:6336646")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.16.14"
            fabricApi("0.92.3+1.20.1")
            mixins {
                disableAutoDetect()
            }
            dependencies {
                localJar("libs/essential-1.20.1.jar")
                modImplementation("curse.maven:yacl-667299:6336639")
                modCompileOnly("curse.maven:modmenu-308702:5162837")
            }
        }
        forge {
            loaderVersion = "47.4.0"
            dependencies {
                localJar("libs/essential-1.20.1.jar")
                modImplementation("curse.maven:yacl-667299:6336646")
                runtimeOnly("curse.maven:essential-mod-546670:8113549")
            }
        }
    }
}
