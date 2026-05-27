plugins {
    id("dev.prism")
}

group = "com.leclowndu93150"
version = "1.0.2"

prism {
    curseMaven()

    metadata {
        modId = "essentialpatcher"
        name = "Essential Patcher"
        description = "Debloats and patches the Essential mod. Unlock cosmetics client-side, disable telemetry, remove ads, and more."
        license = "MIT"
    }

    publishing {
        curseforge {
            accessToken = providers.environmentVariable("CURSEFORGE_TOKEN")
            projectId = "1555663"
        }

        dependencies {
            requires("essential-mod")
            requires("yacl")
        }
    }

    version("1.21.1") {
        common {
            localJar("libs/essential.jar")
            modCompileOnly("curse.maven:yacl-667299:7437845")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.18.6"
            fabricApi("0.116.10+1.21.1")
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

    version("1.20.1") {
        common {
            localJar("libs/essential-1.20.1.jar")
            modCompileOnly("curse.maven:yacl-667299:6336646")
            compileOnly("org.jetbrains.kotlin:kotlin-stdlib:2.1.20")
            compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
        }
        fabric {
            loaderVersion = "0.16.14"
            fabricApi("0.92.3+1.20.1")
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
