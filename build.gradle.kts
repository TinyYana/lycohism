plugins {
    kotlin("jvm") version libs.versions.kotlin.get()
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    // ponytail: compile against local Spigot API to surface all Paper-only calls as errors
    compileOnly(files("D:/02 Games/Minecraft/MinecraftServer/SpigotBuildTool/Spigot/Spigot-API/target/spigot-api-26.2-R0.1-SNAPSHOT-shaded.jar"))
    implementation(libs.kotlin.stdlib)
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        // Produce a single runnable jar (with Kotlin stdlib bundled) as the main
        // artifact, so the plain classifier-less jar is the one to deploy.
        archiveClassifier.set("")
    }

    jar {
        // Avoid a name clash with shadowJar's now classifier-less output.
        archiveClassifier.set("plain")
    }

    test {
        useJUnitPlatform()
    }

    runServer {
        minecraftVersion(libs.versions.minecraft.get())
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version, "description" to project.description)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
