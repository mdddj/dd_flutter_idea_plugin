val dartVersion: String by rootProject
val ideaVersion: String by rootProject
val ideaType: String by rootProject
val pluginVersion: String by rootProject
val type: String by rootProject

plugins {
    kotlin("jvm")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "shop.itbug"
version = pluginVersion

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

intellij {
    version.set(ideaVersion)
    if (ideaType.trim().isNotBlank()) {
        type.set(ideaType)
    }
    plugins.set(listOf("Dart:$dartVersion"))
}