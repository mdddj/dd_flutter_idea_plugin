plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.20"
    id("org.jetbrains.intellij.platform") version "2.10.2"
}

group = "shop.itbug"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

// Read more: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
dependencies {
    // 使用 compileOnly 让编译时可用，但不打包进插件
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    
    implementation("io.modelcontextprotocol:kotlin-sdk-server:0.8.1")
    implementation("io.ktor:ktor-server-core:3.3.3")
    implementation("io.ktor:ktor-server-cio:3.3.3")
    implementation("io.ktor:ktor-server-sse:3.3.3")
    intellijPlatform {
        intellijIdeaCommunity("2025.2.1")
        testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)
        plugins("shop.itbug.FlutterCheckVersionX:6.8.0","Dart:500.0.0")
    }
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = "252.25557"
        }

        changeNotes = """
            <h2>1.0.0 - Initial Release</h2>
            
            <h3>Features / 功能特性</h3>
            <ul>
                <li><b>MCP Server</b> - HTTP server based on Model Context Protocol / 基于 Model Context Protocol 的 HTTP 服务器</li>
                <li><b>Dart VM Tools / Dart VM 工具集</b>
                    <ul>
                        <li>get_running_flutter_apps - Get list of running Flutter apps / 获取运行中的 Flutter 应用列表</li>
                        <li>get_dart_vm_info - Get Dart VM details (version, isolates, architecture) / 获取 Dart VM 详细信息</li>
                        <li>get_http_requests - Get HTTP request monitoring data / 获取 HTTP 请求监控数据</li>
                        <li>get_http_request_detail - Get HTTP request details with body / 获取 HTTP 请求详情</li>
                        <li>get_shared_preferences_keys - Get all SharedPreferences keys / 获取 SharedPreferences 所有 keys</li>
                        <li>get_shared_preferences_value - Get SharedPreferences value by key / 获取 SharedPreferences 指定值</li>
                        <li>get_current_project_name - Get current project name / 获取当前项目名称</li>
                    </ul>
                </li>
                <li><b>Settings Page / 设置页面</b> - Configure port, auto-start, start/stop/restart server / 可配置端口、自动启动，支持启动/停止/重启</li>
                <li><b>SSE Transport / SSE 传输</b> - Server-Sent Events transport protocol support / 支持 SSE 传输协议</li>
            </ul>
        """.trimIndent()
    }
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "21"
        targetCompatibility = "21"
    }

    buildSearchableOptions {
        enabled = false
    }

    runIde {
    }
}

// 打包时排除 IDE 已提供的库，避免类加载器冲突
configurations {
    runtimeClasspath {
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-jvm")
        exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-jdk8")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8")
        exclude(group = "org.slf4j", module = "slf4j-api")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}
