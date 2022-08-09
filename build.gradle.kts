
plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.6.21"
    id("org.jetbrains.intellij") version "1.8.0"
}
//028486
group = "shop.itbug"
version = "2.0.3"

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
    version.set("2021.2.4")


    /// Android studio 是 AI
    /// Idea 社区办 IC
    type.set("IC")
    plugins.set(
        listOf(
            "java",
            "yaml",
            "Dart:212.5744",
            "io.flutter:69.0.2",
            "org.intellij.plugins.markdown:212.5457.16"
        )
    )

}


dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("cn.hutool:hutool-all:5.8.3")
    implementation("org.smartboot.socket:aio-core:1.5.18")
    implementation("com.alibaba:fastjson:2.0.10.graal")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.7.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.6.4")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation ("com.fifesoft:rsyntaxtextarea:3.2.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
    testImplementation("org.slf4j:slf4j-simple:2.0.0-alpha7")
    implementation("org.slf4j:slf4j-api:2.0.0-alpha7")
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
        sinceBuild.set("212")
        untilBuild.set("222.*")
        changeNotes.set("""
            v2.0.2: 新增riverpod代码模板 `conf`和`conl` 
            v2.0.3: 修复了一些小错误
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
