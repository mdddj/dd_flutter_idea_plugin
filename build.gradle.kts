plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.7.20"
    id("org.jetbrains.intellij") version "1.11.0-SNAPSHOT"
}
group = "shop.itbug"
version = "2.1.1"
repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}
java{
    sourceCompatibility = org.gradle.api.JavaVersion.VERSION_17
}

intellij {
    version.set("2022.2.3")
    type.set("IC")
    plugins.set(
        listOf(
            "yaml",
            "Dart:222.4345.14",
            "io.flutter:71.0.5",
            "org.intellij.plugins.markdown:222.4167.22"
        )
    )
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.1")
    implementation("cn.hutool:hutool-all:5.8.8")
    implementation("org.smartboot.socket:aio-core:1.6.0")
    implementation("com.alibaba:fastjson:2.0.14.graal")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.9.0")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0-rc1")
    implementation ("com.github.nkzawa:socket.io-client:0.6.0")
}
var javaVersion = "17"
tasks {
    withType<JavaCompile> {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.jvmTarget = javaVersion
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set("222.4345.14")
        untilBuild.set("223.*")
        changeNotes.set("""
            v2.0.4: 更新pubspec.yaml的快捷图标位置
            
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

    runIde {
        autoReloadPlugins.set(true)
    }


    buildSearchableOptions {
        enabled = false
    }

    compileKotlin {
        kotlinOptions.jvmTarget = "17"
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = "17"
    }
}
