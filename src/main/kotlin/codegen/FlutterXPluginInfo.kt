package codegen
// 自动生成的插件信息类,不要修改这个文件,否则会导致插件功能失效
object FlutterXPluginInfo {
    const val VERSION: String = "7.2.0"
    const val NAME: String = "FlutterX"
    const val CHANGELOG: String = """
<h2>7.2.0 - 2026-07-20</h2>

<h3>🔧 Compatibility</h3>

<ul><li>Upgraded target platform to <strong>IntelliJ IDEA 2026.2</strong> and updated platform Gradle plugin to 2.18.1.</li><li>Upgraded Kotlin to 2.3.20, Compose plugin to 2.3.20, and Gradle wrapper to 9.6.1.</li><li>Updated plugin dependencies: <code>io.flutter:94.0.0</code>, <code>Dart:507.0.0</code>, <code>LSP4IJ:0.20.1</code>.</li><li>Aligned Java and Kotlin JVM targets to 25 to match the IDEA 2026.2 platform JDK.</li><li>Resolved Gradle 9.6 deprecation warnings (<code>by project</code>, <code>by tasks</code>, <code>by tasks.registering</code>).</li><li>Updated plugin verification IDE from IntelliJ IDEA Community 2025.2 to IntelliJ IDEA 2026.2.</li><li>Updated <code>sinceBuild</code> to 262 for IDEA 2026.2 compatibility.</li></ul>

<h3>Improvements</h3>

<ul><li>Added build-time code generation for plugin metadata (<code>FlutterXPluginInfo.kt</code>) and replaced <code>PluginManagerCore</code> runtime lookups with the generated class.</li><li>Replaced <code>IdeBundle</code> message references with <code>PluginBundle</code> in restart confirmation dialogs for consistent i18n.</li><li>Simplified JSON validation utility by removing <code>groovy.json.JsonException</code> dependency in favor of <code>kotlinx.serialization</code>.</li><li>Replaced <code>org.jetbrains.skiko.Cursor</code> with <code>java.awt.Cursor</code> for broader AWT API compatibility.</li><li>Cleaned up stale navbar extension reference from <code>plugin.xml</code>.</li></ul>

<h3>Fixes</h3>

<ul><li>Fixed an EDT thread violation in the L10n window's tree initialization that caused <code>Access is allowed from Event Dispatch Thread (EDT) only</code> exceptions.</li></ul>
"""

}