import org.jetbrains.changelog.Changelog
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val dartVersion: String by project
val sinceBuildVersion: String by project
val untilBuildVersion: String by project
val ideaVersion: String by project
val ideaType: String by project
val pluginVersion: String by project
val type: String by project

plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    id("org.jetbrains.intellij") version "1.17.3"
    idea
    id("org.jetbrains.changelog") version "2.2.0"
}

group = "shop.itbug"
version = pluginVersion + type

repositories {
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
    "org.intellij.plugins.markdown",
    "terminal",
)



intellij {
    version.set(ideaVersion)
    if (ideaType.trim().isNotBlank()) {
        type.set(ideaType)
    }
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
    implementation("cn.hutool:hutool-http:latest.release")
    implementation("org.smartboot.socket:aio-pro:latest.release")
    implementation("com.alibaba.fastjson2:fastjson2:latest.release")
}

val pushToken: String? = System.getenv("idea_push_token")

tasks {

    val myChangeLog = provider {
        changelog.renderItem(
            changelog
                .getOrNull(pluginVersion.removeSuffix(".")) ?: changelog.getUnreleased()
                .withHeader(false)
                .withEmptySections(false),
            Changelog.OutputType.HTML
        )
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    listProductsReleases {
    }

    patchPluginXml {
        sinceBuild.set(sinceBuildVersion)
        untilBuild.set(untilBuildVersion)
        changeNotes.set(myChangeLog)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(pushToken)
    }

    runIde {
        autoReloadPlugins.set(true)
        jvmArgs = listOf("-XX:+AllowEnhancedClassRedefinition")
    }


    buildSearchableOptions {
        enabled = false
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    test {
        useJUnitPlatform()
    }

    configurations.all {
    }

    verifyPlugin {
    }

    verifyPluginConfiguration {
    }

}


changelog {
    version = pluginVersion.removeSuffix(".")
    path = file("CHANGELOG.md").canonicalPath
    groups.empty()
}

