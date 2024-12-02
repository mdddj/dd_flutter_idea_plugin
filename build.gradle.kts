import org.jetbrains.changelog.Changelog
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

val dartVersion: String by project
val sinceBuildVersion: String by project
val untilBuildVersion: String by project
val pluginVersion: String by project
val idePath: String by project

// https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin-dependencies-extension.html


plugins {
    idea
    kotlin("jvm") version "2.1.0"
    id("org.jetbrains.intellij.platform") version "2.1.0"
    id("org.jetbrains.changelog") version "2.2.1"
    id("maven-publish")
//    id("ldd-idea-publisher-plugin")
}

group = "shop.itbug"
version = pluginVersion + sinceBuildVersion

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven { url = uri("https://plugins.gradle.org/m2/") }
    intellijPlatform {
        defaultRepositories()
        releases()
        marketplace()
    }
}


dependencies {
    implementation("org.smartboot.socket:aio-pro:latest.release")
    intellijPlatform {
        local(idePath)
        bundledPlugins(
            "org.jetbrains.plugins.terminal",
            "org.jetbrains.plugins.yaml",
            "org.intellij.plugins.markdown",
//            "com.intellij.modules.json"
        )
        plugins("Dart:$dartVersion")
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }

}


intellijPlatform {
    pluginVerification {
        ides {
//            ide(IntelliJPlatformType.IntellijIdeaCommunity, "243.21155.17")
//            ide(IntelliJPlatformType.IntellijIdeaCommunity, "2024.1.7")
//            local("/Applications/IntelliJ IDEA Ultimate.app")
        }
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

    patchPluginXml {
        sinceBuild.set(sinceBuildVersion)
        untilBuild.set(untilBuildVersion)
        changeNotes.set(myChangeLog)
        pluginDescription.set(file("插件介绍h.md").readText().trim())
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
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Didea.kotlin.plugin.use.k2=true")
        }
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    printProductsReleases {
        sinceBuild.set(sinceBuildVersion)
        untilBuild.set(untilBuildVersion)
    }


    test {
        useJUnitPlatform()
    }

    configurations.all {

    }

    buildSearchableOptions {
        enabled = false
    }
}

changelog {
    version = pluginVersion.removeSuffix(".")
    path = file("CHANGELOG.md").canonicalPath
    groups.empty()
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}


try {
    val userName = System.getenv("maven_username")
    val passWord = System.getenv("maven_password")
    afterEvaluate {
        publishing {
            repositories {
                maven {
                    name = "sonatype"
                    url = uri("https://package.itbug.shop/nexus/repository/idea-plugin/")
                    credentials {
                        username = userName
                        password = passWord
                    }
                }
                publications {
                    create<MavenPublication>("release") {
                        artifact("${layout.buildDirectory}/distributions/FlutterX-${project.version}.zip")
                    }
                }
            }
        }
    }
} catch (e: Exception) {
    println("上传插件到私服失败:${e}")
}

