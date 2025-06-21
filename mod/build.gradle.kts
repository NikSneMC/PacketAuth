plugins {
    id("fabric-loom")
    java
}

loom.runs.forEach { env ->
    env.runDir("run/${env.name}")
    when (env.name) {
        "client" -> env.programArgs.add("--username=TestPacketAuth")
    }
}

group = property("group")!!
version = property("mod_version")!!

configurations {
    create("shade")
    compileClasspath.get().extendsFrom(getByName("shade"))
    implementation.get().extendsFrom(getByName("shade"))
}

dependencies {
    "shade"(project(":common"))
    compileOnly("org.yaml:snakeyaml:2.0")
    minecraft("com.mojang:minecraft:${property("minecraft_version")}")
    mappings("net.fabricmc:yarn:${property("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${property("fabric_version")}")
}

tasks {
    processResources {
        inputs.property("version", version)

        filesMatching("fabric.mod.json") {
            expand(getProperties())
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

    remapJar {
        val minecraft_version: String by project
        val max_minecraft_version: String by project

        archiveBaseName.set("PacketAuth")
        archiveClassifier.set("")
        archiveAppendix.set("FabricQuilt")
        archiveVersion.set(String.format("%s_%s-%s", version, minecraft_version, max_minecraft_version))
    }
}