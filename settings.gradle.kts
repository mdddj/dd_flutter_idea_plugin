rootProject.name = "FlutterX"
//includeBuild("/Users/ldd/IdeaProjects/ldd-idea-publisher")
pluginManagement {
    repositories {
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
