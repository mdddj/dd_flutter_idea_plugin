val dartVersion: String by project
val flutterVersion: String by project
val sinceBuildVersion: String by project
val untilBuildVersion: String by project
val ideaVersion: String by project
val ideaType: String by project
val pluginVersion: String by project

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.20"
    id("org.jetbrains.intellij") version "1.16.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.20-RC"
    idea
}
group = "shop.itbug"
version = pluginVersion

repositories {
    mavenLocal()
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }
}


val pluginList = mutableListOf(
    "yaml",
    "Dart:$dartVersion",
    "io.flutter:$flutterVersion",
    "org.intellij.plugins.markdown",
    "terminal",
)


if (ideaType == "IU" && ideaVersion == "2023.3") {
    pluginList.add("org.jetbrains.android:233.11799.272")
}



intellij {


    version.set(ideaVersion)
    type.set(ideaType)
    plugins.set(pluginList)
}
kotlin {
    sourceSets.all {
        languageSettings {
            languageVersion = "2.0"
        }
    }


}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:latest.release")
    implementation("com.squareup.retrofit2:converter-gson:latest.release")
    implementation("cn.hutool:hutool-all:latest.release")
    implementation("org.smartboot.socket:aio-core:latest.release")
    implementation("com.alibaba.fastjson2:fastjson2:latest.release")
    implementation("com.alibaba.fastjson2:fastjson2-kotlin:latest.release")
    implementation("com.google.code.gson:gson:latest.release")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:latest.release")
}


var javaVersion = "17"
tasks {

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions.jvmTarget = javaVersion
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
        compilerOptions.languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_1_9)
        compilerOptions {

        }
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set(sinceBuildVersion)
        untilBuild.set(untilBuildVersion)
        changeNotes.set(
            """
                <div>
                     <h1>3.9.2</h1>
                     <ul>
                        <li>Fixed the bug that the settings panel could not be opened in 2023.3</li>
                        <li>Adapt to Idea 2023.3.</li>
                     </ul>
                </div>
            """.trimIndent()
        )
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
        jvmArgs = listOf("-XX:+AllowEnhancedClassRedefinition")
    }


    buildSearchableOptions {
        enabled = false
    }

    compileKotlin {
        kotlinOptions.jvmTarget = javaVersion
    }

    compileTestKotlin {
        kotlinOptions.jvmTarget = javaVersion
    }
}


