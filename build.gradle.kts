plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.20"
    id("org.jetbrains.intellij") version "1.5.3"
}

group = "shop.itbug"
version = "1.6-SNAPSHOT"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {


    /// 旧版本支持 idea: 2021.1  Dart:211.7817  io.flutter:66.0.1
    /// 新版本 2022.1   io.flutter:66.0.4 Dart:221.5588
    version.set("2022.1")
    type.set("IC")
    plugins.set(listOf("java","yaml","Dart:221.5588","io.flutter:66.0.4"))
}

dependencies {
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.github.ben-manes.caffeine:caffeine:3.0.6")
    implementation ("cn.hutool:hutool-all:5.8.0.M3")
    implementation("org.smartboot.socket:aio-core:1.5.17")
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
        sinceBuild.set("212")
        untilBuild.set("222.*")
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
