plugins {
    java
}

group = property("group")!!
version = property("mod_version")!!

configurations {
    create("shade")
    compileClasspath.get().extendsFrom(getByName("shade"))
    implementation.get().extendsFrom(getByName("shade"))
}

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") {
        name = "spigotmc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    "shade"(project(":common"))
    compileOnly("org.spigotmc:spigot-api:1.20.5-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.20-R0.2-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
    annotationProcessor("com.velocitypowered:velocity-api:3.3.0-SNAPSHOT")
}

tasks {
    processResources {
        inputs.property("version", version)
        filesMatching(listOf("plugin.yml", "bungee.yml", "velocity-plugin.json")) {
            expand(mutableMapOf("version" to version))
        }
    }

    jar {
        configurations["shade"].forEach { dep ->
            duplicatesStrategy = DuplicatesStrategy.WARN
            from(project.zipTree(dep)) {
                exclude("META-INF/MANIFEST.MF", "module-info.class")
            }
        }
    }

    jar {
        val minecraft_version: String by project

        archiveBaseName.set("PacketAuth")
        archiveClassifier.set("")
        archiveAppendix.set("BukkitBungeeVelocity")
        archiveVersion.set(String.format("%s_%s+", version, minecraft_version))
    }
}