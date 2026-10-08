package codegen
// 自动生成的插件信息类,不要修改这个文件,否则会导致插件功能失效
object FlutterXPluginInfo {
    const val VERSION: String = "7.3.0"
    const val NAME: String = "FlutterX"
    const val CHANGELOG: String = """
<h2>7.3.0 - 2026-10-08</h2>

<h3>New Features</h3>

<ul><li>Added MCP tools for Dart VM data: <code>flutterx_list_apps</code>, <code>flutterx_vm_info</code>, and <code>flutterx_list_data_tools</code> let AI agents discover running Flutter apps and inspect the VM.</li><li>Added an optional <code>DartVmMcpToolExtension</code> interface so any Dart VM dev tool tab can expose its own MCP tools; toolsets are collected automatically.</li><li>Added read-only MCP tools for every built-in Dart VM tab: <code>flutterx_vm_status</code>, <code>flutterx_logs</code>, <code>flutterx_memory</code>, <code>flutterx_http</code>, <code>flutterx_providers</code>, <code>flutterx_riverpod</code>, <code>flutterx_shared_preferences</code>, <code>flutterx_hive</code>, and <code>flutterx_drift</code>.</li></ul>

<h3>Improvements</h3>

<ul><li>Made the Riverpod panel easier to scan: frames start at 1, same-name providers stay distinct, and short value changes show as before/after cards.</li><li>Updated bundled IDE plugin dependencies to Dart 510 and Flutter 97.</li></ul>

<h3>Removed</h3>

<ul><li>Removed the Android Gradle Migrate tool from the FlutterX tool window.</li><li>Removed the Enum Migrate tool from the FlutterX tool window.</li><li>Removed Aliyun Gradle mirror inlays for Groovy and Kotlin DSL build files.</li><li>Removed the bundled Groovy plugin dependency.</li><li>Removed the bundled Kotlin IDE plugin dependency.</li><li>Removed the FlutterX-MCP companion plugin from <code>extras</code> and from the release workflow.</li></ul>
"""

}