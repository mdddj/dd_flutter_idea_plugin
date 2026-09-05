package codegen
// 自动生成的插件信息类,不要修改这个文件,否则会导致插件功能失效
object FlutterXPluginInfo {
    const val VERSION: String = "7.2.1"
    const val NAME: String = "FlutterX"
    const val CHANGELOG: String = """
<h2>7.2.1 - 2026-09-05</h2>

<h3>New Features</h3>

<ul><li>Added a frame-based Riverpod DevTool experience aligned with the official <code>riverpod_devtool</code>, including provider status grouping, state diffs, state trees, event history, and hot-restart recovery.</li><li>Added a Flutter SDK installer flow with channel and version selection, download progress, and installation support.</li><li>Added pubspec inlay extension points so package metadata and actions can be contributed independently.</li><li>Added AGP 9 built-in Kotlin compatibility detection for installed Flutter packages.</li></ul>

<h3>Improvements</h3>

<ul><li>Improved Riverpod DevTool state tracking with incremental event updates and provider dependency accumulation.</li><li>Added richer pubspec package inlays and improved YAML path resolution behavior.</li><li>Updated Dart and Flutter plugin dependencies for the current IntelliJ platform.</li></ul>

<h3>Fixes</h3>

<ul><li>Fixed Dart 509 compatibility by migrating from the removed synchronous <code>analysis_getHover</code> API to the Dart LSP hover API.</li><li>Fixed semantic and syntax highlighting failures in large Dart files caused by the obsolete hover API.</li><li>Fixed LSP4J class loader conflicts by removing the redundant lsp4ij dependency and using the Dart plugin's LSP implementation.</li></ul>
"""

}