import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

val dartVersion: String by project
val sinceBuildVersion: String by project
val untilBuildVersion: String by project
val pluginVersion: String by project
val ideType: String by project


plugins {
    idea
    kotlin("jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.9.0"
    id("org.jetbrains.changelog") version "2.2.1"
    id("maven-publish")
    id("org.jetbrains.compose") version "1.8.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.20"
}

group = "shop.itbug"
version = pluginVersion + ideType

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
    maven { url = uri("https://plugins.gradle.org/m2/") }
    intellijPlatform {
        defaultRepositories()
        releases()
        marketplace()
        androidStudioInstallers()
        intellijDependencies()
    }
}


val bPlugins = mutableListOf(
    "org.jetbrains.plugins.terminal",
    "org.jetbrains.plugins.yaml",
    "org.intellij.plugins.markdown",
    "org.intellij.groovy"
)

if (ideType.toInt() >= 243) {
    println("大于 243")
    bPlugins.add("com.intellij.modules.json")
    bPlugins.add("com.intellij.platform.images")
    if(ideType != "253"){
        bPlugins.add("org.intellij.intelliLang")
    }

}

dependencies {
    implementation("org.smartboot.socket:aio-pro:latest.release")
    testImplementation("junit:junit:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")

    intellijPlatform {
        when (ideType) {
            "253" -> {
//                intellijIdeaCommunity("2025.2.1")
                local("/Users/ldd/Applications/IntelliJ IDEA Ultimate.app")
            }
            "252" -> {
                intellijIdeaCommunity("2025.2.1")
//                local("/Users/ldd/Applications/IntelliJ IDEA Ultimate.app")
            }

            "251" -> {
//                intellijIdeaCommunity("2025.1.4.1")
                local("/Applications/Android Studio.app")
            }
        }
        bundledPlugins(bPlugins)
        //"io.flutter:87.1"
        plugins("Dart:$dartVersion", "io.flutter:87.1")
        pluginVerifier()
        zipSigner()
        javaCompiler()

        if(ideType == "253"){
//            bundledModule("org.intellij.intelliLang")
        }

        bundledModule("intellij.libraries.ktor.client")
        bundledModule("intellij.libraries.ktor.client.cio")

        testBundledModules("intellij.libraries.ktor.client", "intellij.libraries.ktor.client.cio")

        testFramework(TestFrameworkType.Platform)



        // jewel
        bundledModule("intellij.platform.jewel.foundation")
        bundledModule("intellij.platform.jewel.ui")
        bundledModule("intellij.platform.jewel.ideLafBridge")
        bundledModule("intellij.platform.jewel.markdown.core")
        bundledModule("intellij.platform.jewel.markdown.ideLafBridgeStyling")
        bundledModule("intellij.libraries.compose.foundation.desktop")
        bundledModule("intellij.libraries.skiko")
    }
    testImplementation("junit:junit:4.13.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

}


intellijPlatform {
    pluginVerification {
        ides {
            create(IntelliJPlatformType.IntellijIdeaCommunity, "2025.2")
        }
    }
}


kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
        freeCompilerArgs.add("-Xwhen-guards")
        freeCompilerArgs.add("-Xnon-local-break-continue")
        freeCompilerArgs.add("-Xmulti-dollar-interpolation")
    }
}
val pushToken: String? = System.getenv("idea_push_token")
val myChangeLog = provider {
    changelog.renderItem(
        changelog.getOrNull(pluginVersion.removeSuffix(".")) ?: changelog.getUnreleased().withHeader(false)
            .withEmptySections(false), Changelog.OutputType.HTML
    )
}

val currentVersionChangelog = provider {
    changelog.renderItem(
        changelog.getOrNull(pluginVersion.removeSuffix(".")) ?: changelog.getUnreleased().withHeader(false)
            .withEmptySections(false), Changelog.OutputType.HTML
    )
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xmulti-dollar-interpolation", "-Xwhen-guards"))
}

tasks {

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
//        channels.set(listOf("bata","stable"))
    }

    runIde {
        args = listOf("/Users/hlx/github/dd_flutter_idea_plugin/flutterdemo")
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf(
                "-Didea.kotlin.plugin.use.k2=true",
                "-Didea.log.level=DEBUG",
                "-Didea.log.debug=true",
                "-Didea.log.verbose=true",
                "-Dlog4j.logger.shop.itbug.fluttercheckversionx=DEBUG"
            )
        }
        systemProperty("idea.log.trace.categories", "shop.itbug.fluttercheckversionx")
    }

    compileKotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
        }
    }

    printProductsReleases {
        sinceBuild.set(sinceBuildVersion)
        untilBuild.set(untilBuildVersion)
    }


    configurations.all {

    }

    buildSearchableOptions {
        enabled = false
    }
}

val getChannel = tasks.publishPlugin.get().channels.get()

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


idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}


// 代码生成插件的基本信息

val generateFlutterPluginInfo by tasks.registering {
    group = "codegen"
    description = "Generates FlutterPluginInfo class with version info"

    println(
        """
        changelog : \n
        ${currentVersionChangelog.get()}
    """.trimIndent()
    )

    // 定义输出目录和文件路径
    val outputDir = file("src/main/kotlin/codegen")
    val outputFile = File(outputDir, "FlutterXPluginInfo.kt")
    if (outputFile.exists()) {
        outputFile.delete()
    }
    if (outputDir.exists().not()) {
        outputDir.mkdirs()
    }

    // 设置输入和输出以支持增量构建
//    outputs.file(outputFile)
//
//    doLast {
//        val q = "\"\"\"\n"
//        outputFile.writeText(
//            """
//            |package codegen
//            |// 自动生成的插件信息类,不要修改这个文件,否则会导致插件功能失效
//            |object FlutterXPluginInfo {
//            |    const val VERSION: String = "${project.version}"
//            |    const val CHANGELOG: String = $q${currentVersionChangelog.get()}$q
//            |}
//            |""".trimMargin().trimIndent()
//        )
//    }
}

// 让 Kotlin 编译任务依赖生成任务
tasks.compileKotlin {
    dependsOn(generateFlutterPluginInfo)
}
tasks.buildPlugin {
    dependsOn(generateFlutterPluginInfo)
}
tasks.publishPlugin {
    dependsOn(generateFlutterPluginInfo)
}
tasks.runIde {
    dependsOn(generateFlutterPluginInfo)
}
tasks.prepareKotlinBuildScriptModel {
    dependsOn(generateFlutterPluginInfo)
}
// 清理生成的文件（可选）
tasks.clean {
    delete("src/main/kotlin/codegen/FlutterPluginInfo.kt")
}

