package codegen
// 自动生成的插件信息类,不要修改这个文件,否则会导致插件功能失效
object FlutterXPluginInfo {
    const val VERSION: String = "7.3.1"
    const val NAME: String = "FlutterX"
    const val CHANGELOG: String = """
<h2>7.3.1 - 2026-10-09</h2>

<h3>Fixes</h3>

<ul><li>Fixed a startup freeze when <code>pubspec.yaml</code> was already open. The IDE stopped responding, and the close button did nothing until the process was killed. Pubspec notifications now read the file on the current thread, and dependency gutter icons are created only after the project has finished opening and the document is committed.</li></ul>

<h3>Improvements</h3>

<ul><li>Replaced obsolete background and modal progress tasks with the IntelliJ 2026.2 progress APIs. Flutter version checks, package lookup, pub get, publish, downloads, and Freezed project scans use the new progress UI.</li><li>Localized the Flutter version check progress title in Simplified Chinese, English, Traditional Chinese, Japanese, and Korean.</li></ul>

<h3>Automation</h3>

<ul><li>Release builds now use JBR 25, matching the Java 25 compile target.</li><li>A successful release now opens a pull request into <code>master</code>.</li></ul>
"""

}