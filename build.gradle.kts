import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val dartVersion: String by project
val sinceBuildVersion: String by project
val untilBuildVersion: String by project
val pluginVersion: String by project


plugins {
    idea
    kotlin("jvm") version "2.0.0"
    id("org.jetbrains.intellij.platform") version "2.3.0"
    id("org.jetbrains.changelog") version "2.2.1"
    id("maven-publish")
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
        androidStudioInstallers()
        intellijDependencies()
    }
}


val bPlugins = mutableListOf(
    "org.jetbrains.plugins.terminal",
    "org.jetbrains.plugins.yaml",
    "org.intellij.plugins.markdown",
    "com.intellij.gradle",
    "org.jetbrains.plugins.gradle"
)

if (sinceBuildVersion.toInt() >= 243) {
    bPlugins.add("com.intellij.modules.json")
}

dependencies {
    implementation("org.smartboot.socket:aio-pro:latest.release")
    testImplementation("junit:junit:latest.release")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:latest.release")
    intellijPlatform {
        testFramework(TestFrameworkType.Platform)
        when (sinceBuildVersion) {
            "243" -> {
                local("/Applications/IntelliJ IDEA Ultimate.app")
            }

            "251" -> {
                local("/Applications/IntelliJ IDEA Ultimate 2025.1 EAP.app")
            }

            else -> {
//                local("/Applications/Android Studio.app")
                androidStudio("2024.2.2.13")
            }
        }
        bundledPlugins(bPlugins)
        plugins("Dart:$dartVersion")
        pluginVerifier()
        zipSigner()
        javaCompiler()

    }

}


intellijPlatform {
    pluginVerification {
        ides {
            when (sinceBuildVersion) {
                "243" -> {
                    local("/Applications/IntelliJ IDEA Ultimate.app")
                }

                "251" -> {
                    local("/Applications/IntelliJ IDEA Ultimate 2025.1 EAP.app")
                }

                else -> {
                    local("/Applications/Android Studio.app")
                }
            }
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

val currentTime: String = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

val compileKotlin: KotlinCompile by tasks
compileKotlin.compilerOptions {
    freeCompilerArgs.set(listOf("-Xmulti-dollar-interpolation"))
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
    }

    runIde {
        jvmArgs = listOf("-XX:+AllowEnhancedClassRedefinition")
        jvmArgumentProviders += CommandLineArgumentProvider {
            listOf("-Didea.kotlin.plugin.use.k2=true", "-Didea.log.level=DEBUG")
        }
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


idea {
    module {
        isDownloadSources = true
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
    outputs.file(outputFile)

    doLast {
        val q = "\"\"\"\n"
        outputFile.writeText(
            """
            |package codegen
            |
            |object FlutterXPluginInfo {
            |    const val Version: String = "${project.version}"
            |    const val Changelog: String = $q${currentVersionChangelog.get()}$q
            |    const val BuildDate: String = "$currentTime"
            |}
            |""".trimMargin().trimIndent()
        )
    }
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



intellijPlatformTesting {
    runIde {
        register("runIdeForUiTests") {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Drobot-server.port=8082",
                        "-Dide.mac.message.dialogs.as.sheets=false",
                        "-Djb.privacy.policy.text=<!--999.999-->",
                        "-Djb.consents.confirmation.enabled=false",
                    )
                }
            }

            plugins {
                robotServerPlugin()
            }
        }
    }
}

sourceSets {
    create("integrationTest") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val integrationTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}

dependencies {
    integrationTestImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    integrationTestImplementation("org.kodein.di:kodein-di-jvm:7.20.2")
    integrationTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:1.10.1")
}

val integrationTest = tasks.register<Test>("integrationTest", fun Test.() {
    val integrationTestSourceSet = sourceSets.getByName("integrationTest")
    testClassesDirs = integrationTestSourceSet.output.classesDirs
    classpath = integrationTestSourceSet.runtimeClasspath
    systemProperty("./build", tasks.prepareSandbox.get().pluginDirectory.get().asFile)
    useJUnitPlatform()
    dependsOn(tasks.prepareSandbox)
})