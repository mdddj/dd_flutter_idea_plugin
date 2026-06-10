---
name: flutterx-dart-vm-devtool-extension
description: Use when implementing, reviewing, or explaining third-party IntelliJ plugin extensions for FlutterX Dart VM ToolWindow dev tool tabs through the shop.itbug.FlutterCheckVersionX.dartVmDevTool extension point.
---

# FlutterX Dart VM Dev Tool Extension

## Purpose

Use this skill when a task involves extending the FlutterX Dart VM ToolWindow with third-party dev tool tabs, changing the FlutterX Dart VM extension point API, or explaining how another IntelliJ plugin can contribute a Dart VM tool.

FlutterX exposes the `shop.itbug.FlutterCheckVersionX.dartVmDevTool` extension point. Built-in tabs and third-party tabs are registered through the same mechanism.

## Current Public API

Core API files in FlutterX:

- `src/main/kotlin/shop/itbug/flutterx/api/vm/DartVmDevToolExtension.kt`
- `src/main/kotlin/shop/itbug/flutterx/api/vm/DartVmDevToolContext.kt`
- `src/main/kotlin/shop/itbug/flutterx/api/vm/DartVmApp.kt`
- `src/main/kotlin/shop/itbug/flutterx/api/vm/DartVmDevToolExtensionBean.kt`

The extension interface is:

```kotlin
interface DartVmDevToolExtension {
    fun getTabTitle(project: Project): String
    fun isAvailable(context: DartVmDevToolContext): Boolean = true
    fun createComponent(context: DartVmDevToolContext): JComponent
}
```

The context exposes:

```kotlin
interface DartVmDevToolContext {
    val project: Project
    val toolWindow: ToolWindow
    val runningApps: StateFlow<List<DartVmApp>>
}
```

Each app exposes:

```kotlin
interface DartVmApp {
    val appId: String
    val vmUrl: String
    val deviceId: String
    val mode: String
    val vmService: VmService
}
```

`runningApps` is live state. UI code should subscribe to it when it needs to react to Flutter app launch/stop after the ToolWindow is already open.

## Third-Party plugin.xml

The third-party plugin must depend on FlutterX and register a `dartVmDevTool` extension under FlutterX's namespace:

```xml
<depends>shop.itbug.FlutterCheckVersionX</depends>

<extensions defaultExtensionNs="shop.itbug.FlutterCheckVersionX">
    <dartVmDevTool
        id="myTool"
        implementation="com.example.MyDartVmDevTool"
        descriptionKey="my.tool.description"
        order="after hive"/>
</extensions>
```

Attributes:

- `id` is required and must be stable. FlutterX stores hidden tab settings by this id.
- `implementation` is required and must implement `DartVmDevToolExtension`.
- `descriptionKey` is optional. When present, FlutterX resolves it from the contributing plugin's resource bundle and shows it as the settings page comment.
- `order` follows IntelliJ extension ordering, for example `first`, `last`, `after hive`, `before drift`.

If `descriptionKey` is omitted or missing from the bundle, FlutterX simply does not show a description comment.

## Implementation Template

Keep extension classes stateless. Do not do heavy work in the constructor.

```kotlin
package com.example

import com.intellij.openapi.project.Project
import shop.itbug.flutterx.api.vm.DartVmDevToolContext
import shop.itbug.flutterx.api.vm.DartVmDevToolExtension
import java.awt.BorderLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JComponent

class MyDartVmDevTool : DartVmDevToolExtension {
    override fun getTabTitle(project: Project): String = "My Tool"

    override fun isAvailable(context: DartVmDevToolContext): Boolean {
        return true
    }

    override fun createComponent(context: DartVmDevToolContext): JComponent {
        return JPanel(BorderLayout()).apply {
            add(JLabel("My FlutterX Dart VM tool"), BorderLayout.CENTER)
        }
    }
}
```

For reactive UI, collect `context.runningApps` using the UI stack's lifecycle. Compose users can collect it in a composable. Swing users should create a coroutine tied to a `Disposable` and update Swing on the EDT.

## Settings Integration

FlutterX automatically lists registered Dart VM tabs in the Dart VM settings page. Third-party tabs are shown by default.

Users can hide a tab by unchecking it. FlutterX stores hidden tab ids, so changing a published `id` will reset user preferences and should be treated as a breaking change.

The ToolWindow only creates content for visible tabs. Hidden third-party tabs are skipped before instantiating or rendering their component.

## Behavior Notes

- FlutterX v1 does not support dynamic plugin install/uninstall refreshing already-open Dart VM tabs.
- The extension returns `JComponent`, not Compose types, to avoid leaking FlutterX's Compose/Jewel version choices into third-party plugins.
- If a third-party extension throws while creating a tab, FlutterX logs the implementation class and skips that tab without blocking other tabs.
- Built-in tabs use the same extension point. This is useful when debugging order, visibility, and settings behavior.
- `DartVmApp.vmService` exposes the Dart VM RPC surface. Treat calls as potentially slow or failing; do not block the EDT.

## Validation Checklist

When changing this API or adding a third-party example, run:

```bash
./gradlew compileKotlin
./gradlew verifyPluginStructure
git diff --check
```

For manual validation:

- Open `FlutterX Dart VM`.
- Confirm built-in tabs still appear in order.
- Run a Flutter app after opening the ToolWindow and verify UI observing `context.runningApps` updates.
- Hide/show the third-party tab in Dart VM settings and verify the ToolWindow refreshes.
- Register a test extension with `order="after hive"` and confirm ordering.
- Make one test extension throw from `createComponent` and confirm other tabs still load.

## FlutterX Files To Inspect

Start with these files when debugging or modifying the extension point:

- `src/main/resources/META-INF/plugin.xml`
- `src/main/kotlin/shop/itbug/flutterx/window/DartVmServiceWindowsFactory.kt`
- `src/main/kotlin/shop/itbug/flutterx/setting/vm/DartVmSetting.kt`
- `src/main/kotlin/shop/itbug/flutterx/window/vm/extension/BuiltinDartVmDevToolExtensions.kt`
- `src/main/kotlin/shop/itbug/flutterx/window/vm/extension/DartVmDevToolContextImpl.kt`
