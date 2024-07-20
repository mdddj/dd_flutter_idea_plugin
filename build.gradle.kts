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

// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html
plugins {
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.0.0-RC1"
    id("org.jetbrains.changelog") version "2.2.0"
}

group = "shop.itbug"
version = pluginVersion + type

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven { url = uri("https://plugins.gradle.org/m2/") }
    //新增
    intellijPlatform {
        defaultRepositories()
        releases()
        marketplace()
    }
}


val pluginList = mutableListOf(
    "yaml",
    "Dart:$dartVersion",
    "org.intellij.plugins.markdown",
    "terminal",
)

dependencies {
    implementation("org.smartboot.socket:aio-pro:latest.release")
    intellijPlatform {
        intellijIdeaCommunity(ideaVersion)
        bundledPlugins("org.jetbrains.plugins.terminal", "org.jetbrains.plugins.yaml", "org.intellij.plugins.markdown")
        plugins("Dart:$dartVersion")
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

val pushToken: String? = System.getenv("idea_push_token")

tasks {
    val myChangeLog = provider {
        changelog.renderItem(
            changelog.getOrNull(pluginVersion.removeSuffix(".")) ?: changelog.getUnreleased().withHeader(false)
                .withEmptySections(false), Changelog.OutputType.HTML
        )
    }
    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            jvmTarget.set(JvmTarget.JVM_17)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }


    printBundledPlugins {}

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

}


changelog {
    version = pluginVersion.removeSuffix(".")
    path = file("CHANGELOG.md").canonicalPath
    groups.empty()
}