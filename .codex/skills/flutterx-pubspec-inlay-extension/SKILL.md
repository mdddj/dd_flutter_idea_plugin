---
name: flutterx-pubspec-inlay-extension
description: Use when implementing, reviewing, or explaining third-party IntelliJ plugin inlays for Flutter pubspec.yaml files through the shop.itbug.FlutterCheckVersionX.pubspecInlayProvider extension point.
---

# FlutterX Pubspec Inlay Extension

## Purpose

FlutterX exposes one plugin-owned extension point for features that add inline presentations to dependency entries in `pubspec.yaml`. A contributing plugin implements `PubspecInlayProvider` and receives a unified context containing the current YAML PSI and, when applicable, package metadata.

Contributing plugins must register `pubspecInlayProvider`; they should not register additional IDEA `codeInsight.inlayProvider` implementations. FlutterX keeps the single IDEA provider as an internal adapter and dispatches to all registered FlutterX providers.

## Public API

Start with these files in the FlutterX repository:

- `src/main/kotlin/shop/itbug/flutterx/api/inlay/PubspecInlayProvider.kt`
- `src/main/kotlin/shop/itbug/flutterx/inlay/yaml/FlutterXPubspecInlayProvider.kt`
- `src/main/resources/META-INF/plugin.xml`

The extension point name is:

```text
shop.itbug.FlutterCheckVersionX.pubspecInlayProvider
```

The provider contract is:

```kotlin
interface PubspecInlayProvider : DumbAware {
    fun collect(context: PubspecInlayContext)
}
```

`PubspecInlayContext` exposes:

- `file`: the current `YAMLFile`.
- `element`: the PSI element currently being collected.
- `editor`: the current editor.
- `factory`: IDEA's `PresentationFactory` for building an inlay presentation.
- `packageContext`: package information, or `null` when the element is not a supported dependency entry.

Use `context.addInlineElement(...)` to add an inlay. It places the presentation at `element.textRange.endOffset` by default. Supply `offset`, `relatesToPrecedingText`, or `placeAtTheEndOfLine` when the presentation needs different anchoring.

## Package Context

`packageContext` is created only for direct package entries under these `pubspec.yaml` sections:

- `dependencies`
- `dev_dependencies`
- `dependency_overrides`

The built-in package filter excludes `flutter` and `flutter_localizations`. For all other YAML nodes, section names, and unrelated top-level fields, return immediately when `packageContext` is `null`.

`PubspecPackageContext` contains:

| Property | Meaning |
| --- | --- |
| `name` | The package key, such as `provider`. |
| `element` | The package's `YAMLKeyValueImpl`. |
| `yamlExtends` | The shared `YamlExtends` wrapper for dependency-specific YAML inspection. |
| `dartYamlModel` | Parsed local dependency model, when available. Nullable. |
| `pubVersionDataModel` | Cached/fetched pub.dev package data attached to the Dart model. Nullable. |

The nullable fields are intentional. A scalar constraint such as `provider: ^6.1.1` can provide a local or cached `DartYamlModel`; it may still be unavailable while the PSI or cache is incomplete. Structured `path` and `git` dependencies still receive a package context and `YamlExtends`, but the current adapter does not create a `DartYamlModel` or `PubVersionDataModel` for them. The `any` constraint and invalid or incomplete YAML can also leave model data `null`.

Do not repeat dependency-section detection or package model lookup in each provider. Read the shared context instead. Use `yamlExtends` for path/git-specific helpers, `dartYamlModel` for the declared version and PSI pointers, and `pubVersionDataModel` for package release information.

## Registration

The contributing plugin must depend on FlutterX and register its implementation under FlutterX's namespace:

```xml
<depends>shop.itbug.FlutterCheckVersionX</depends>

<extensions defaultExtensionNs="shop.itbug.FlutterCheckVersionX">
    <pubspecInlayProvider
        id="myPubspecFeature"
        implementation="com.example.MyPubspecInlayProvider"
        order="after pathPackage"/>
</extensions>
```

Keep `id` stable. FlutterX uses extension ordering when dispatching providers; use `first`, `last`, `before <id>`, or `after <id>` only when ordering affects the result. Do not add a separate `<codeInsight.inlayProvider>` entry for this feature.

## Implementation Template

Keep the provider stateless and do not perform network, indexing, or other expensive work in its constructor or directly on the editor thread. Treat PSI and model pointers as potentially invalid between collection passes.

```kotlin
package com.example

import shop.itbug.flutterx.api.inlay.PubspecInlayContext
import shop.itbug.flutterx.api.inlay.PubspecInlayProvider

class MyPubspecInlayProvider : PubspecInlayProvider {
    override fun collect(context: PubspecInlayContext) {
        val packageContext = context.packageContext ?: return

        val packageName = packageContext.name
        val yaml = packageContext.yamlExtends
        val dartModel = packageContext.dartYamlModel
        val pubData = packageContext.pubVersionDataModel

        // Decide what to render from the unified package context.
        val text = pubData?.latest?.version ?: dartModel?.version ?: return
        context.addInlineElement(
            presentation = context.factory.inset(
                context.factory.smallText("$packageName $text"),
                left = 5,
            ),
        )
    }
}
```

Use `yaml` only when the feature needs YAML-specific behavior, for example:

```kotlin
if (!packageContext.yamlExtends.isPathElement()) return
```

For a feature that targets non-package YAML nodes, the same provider can inspect `context.element` directly, but it should not assume `packageContext` is present.

## Behavior and Failure Handling

- The adapter invokes providers while collecting each YAML PSI element in `pubspec.yaml`.
- Providers should be fast and side-effect-light; defer network or disk work to an appropriate background service and let a later collection pass consume refreshed data.
- `ProcessCanceledException` is rethrown so IntelliJ cancellation works correctly.
- Other provider failures are logged with the provider class name and do not prevent other registered providers from rendering.
- Keep each feature in its own provider. Shared package discovery, model lookup, and inlay insertion belong to the FlutterX API/context.

## Validation

For a provider or registration change, run:

```bash
./gradlew compileKotlin --no-daemon
./gradlew verifyPluginStructure --no-daemon
git diff --check
```

When changing FlutterX's extension-point API or the host adapter, also run the relevant tests and `./gradlew verifyPlugin --no-daemon`. For package-context behavior, use `PubspecPackageContextTest` as the reference test.

## FlutterX Files To Inspect

- `src/main/kotlin/shop/itbug/flutterx/api/inlay/PubspecInlayProvider.kt`
- `src/main/kotlin/shop/itbug/flutterx/inlay/yaml/FlutterXPubspecInlayProvider.kt`
- `src/main/kotlin/shop/itbug/flutterx/util/YamlExtends.kt`
- `src/main/kotlin/shop/itbug/flutterx/common/yaml/DartYamlModel.kt`
- `src/main/kotlin/shop/itbug/flutterx/model/PubVersionDataModel.kt`
- `src/main/resources/META-INF/plugin.xml`
