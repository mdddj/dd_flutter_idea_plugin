
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("org.jetbrains.intellij") version "1.6.0"
}
//028486
group = "shop.itbug"
version = "1.9.3-AS"

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
    version.set("2021.2.1")


    /// Android studio 是 AI
    /// Idea 社区办 IC
    type.set("IC")
    plugins.set(
        listOf(
            "java",
            "yaml",
            "Dart:212.5080.8",
            "io.flutter:61.2.4",
            "org.intellij.plugins.markdown:212.5080.22"
        )
    )
}


dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("cn.hutool:hutool-all:5.8.3")
    implementation("org.smartboot.socket:aio-core:1.5.18")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
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


    listProductsReleases {
//        sinceVersion.set("2021.2.1")
//        untilVersion.set("2021.2.1")
        sinceBuild.set("212.5712.43.2112.8609683")
        untilBuild.set("212.5712.43.2112.8609683")
    }

    patchPluginXml {
        changeNotes.set("""
            优化dio请求窗口
        """.trimIndent())
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
