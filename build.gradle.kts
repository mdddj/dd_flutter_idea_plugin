import org.jetbrains.compose.compose

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("org.jetbrains.intellij") version "1.6.0"
    id("org.jetbrains.compose") version "1.2.0-alpha01-dev683"
}
//028486
group = "shop.itbug"
version = "1.8.3"

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {


    /// 旧版本支持 idea: 2021.1  Dart:211.7817  io.flutter:66.0.1
    /// 新版本 2022.1   io.flutter:66.0.4 Dart:221.5588
    version.set("2022.1")
    type.set("IC")
    plugins.set(
        listOf(
            "java",
            "yaml",
            "Dart:221.5588",
            "io.flutter:68.1.4",
            "org.intellij.plugins.markdown:221.5080.126"
        )
    )
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.0")
    implementation("cn.hutool:hutool-all:5.8.0")
    implementation("org.smartboot.socket:aio-core:1.5.17")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.1")
}



tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = "11"
    }

    patchPluginXml {
        sinceBuild.set("221")
        untilBuild.set("221.*")

    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
