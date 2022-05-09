plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.titanrealms.minestom"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://jitpack.io")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation("com.github.Minestom:Minestom:64de8f87c0")
    implementation("net.kyori:adventure-text-minimessage:4.10.1")

    implementation("net.titanrealms.api:java-client:1.0")
}

tasks {
    withType<Jar> {
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "net.titanrealms.minestom.server.TestRun" // This should be prod run in the future
                )
            )
        }
    }
}