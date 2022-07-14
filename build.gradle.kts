
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("org.jetbrains.intellij") version "1.7.0"
}
//028486
group = "shop.itbug"
version = "1.9.4"

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
    version.set("2022.1.3")


    /// Android studio 是 AI
    /// Idea 社区办 IC
    type.set("IC")
    plugins.set(
        listOf(
            "java",
            "yaml",
            "Dart:221.5921.27",
            "io.flutter:69.0.4",
            "org.intellij.plugins.markdown:221.5080.126"
        )
    )

}


dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("cn.hutool:hutool-all:5.8.3")
    implementation("org.smartboot.socket:aio-core:1.5.18")
    implementation("com.alibaba:fastjson:2.0.8.graal")
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

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set("221")
        untilBuild.set("221.*")
        changeNotes.set("""
            
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
