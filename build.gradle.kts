plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.8.0"
    id("org.jetbrains.intellij") version "1.12.0"
}
group = "shop.itbug"
version = "2.1.6"
repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}
java{
    sourceCompatibility = JavaVersion.VERSION_17
}

intellij {
    version.set("2022.3")
    type.set("IC")
    plugins.set(
        listOf(
            "yaml",
            "Dart:223.7571.203",
            "io.flutter:71.3.6",
            "org.intellij.plugins.markdown:223.7571.125",
            "terminal","java"
        )
    )
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.squareup.retrofit2:converter-scalars:2.9.0")
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.2")
    implementation("cn.hutool:hutool-all:5.8.11")
    implementation("org.smartboot.socket:aio-core:1.6.1")
    implementation("com.alibaba.fastjson2:fastjson2:2.0.22")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:2.0.22")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.14.0")
    implementation("org.hildan.krossbow:krossbow-stomp-core:4.5.0")
    implementation("org.hildan.krossbow:krossbow-websocket-okhttp:4.5.0")
    implementation("com.google.guava:guava:31.1-jre")
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
        sinceBuild.set("223.7571.182")
        untilBuild.set("231.*")
        changeNotes.set("""
            <div>
                <h1>2023-01-07</h1>
            </div>
            <ul>
                <li>Add the function of automatically generating asset file objects</li>
            </ul>
            <hr>
            <div>
                <h1>2023-01-06</h1>
            </div>
            <ul>
                <li>Fix the problem that some dio interfaces cannot listen</li>
                <li>Optimize the asset file pop-up mechanism</li>
            <ul>
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
