rootProject.name = "PacketAuth"

pluginManagement {
    repositories {
        maven("https://maven.fabricmc.net/") {
            name = "Fabric"
        }
        mavenCentral()
        gradlePluginPortal()
    }

    val loom_version: String by settings
    plugins {
        id("fabric-loom") version loom_version
    }
}

include("common", "plugin", "mod")
