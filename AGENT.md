# AGENT.md

Guidance for AI agents and contributors working on FlutterX (`dd_flutter_idea_plugin`).

## Non-negotiable: never use `@ApiStatus.Internal` APIs

The JetBrains Marketplace **rejects** plugins that call internal platform APIs. `./gradlew verifyPlugin` fails the build with `INTERNAL_API_USAGES` before a release is published, so an internal-API call is a hard blocker, not a warning.

**Rule:** do not call, override, or register anything annotated `@org.jetbrains.annotations.ApiStatus.Internal`.

Related annotations have their own consequences — treat them as signals to find an alternative:

| Annotation | Action |
| --- | --- |
| `@ApiStatus.Internal` | Never use. Release blocker. |
| `@ApiStatus.ScheduledForRemoval` / `@Obsolete` | Find the replacement now; it will break. |
| `@ApiStatus.Experimental` | Allowed, but expect breakage; keep usage small and isolated. |

### Check before committing

```bash
./gradlew verifyPlugin
```

A clean run prints `Compatible` and does **not** create `internal-api-usages.txt` under `build/reports/pluginVerifier/`. Any entry in that file is a release blocker, even if everything compiles, tests pass, and the IDE runs fine locally — only the verifier and Marketplace check this.

### Verify an annotation yourself

Annotated members are easy to miss in source, because JetBrains annotates individual overloads and whole interfaces. Inspect the compiled class directly:

```bash
# locate the jar, then extract and inspect
javap -v -p -cp path/to/classes com.intellij.SomeClass | grep -B3 -A3 'ApiStatus\$Internal'
```

### Known traps hit in this repository

These all compiled and ran fine, then failed `verifyPlugin` or were silently wrong:

- **`com.intellij.ide.ApplicationInitializedListener`** — the whole interface is `@ApiStatus.Internal` (its `componentsInitialized()` is also `@Deprecated("Use [execute]")`). Do not register `applicationInitializedListener`. For startup work, initialize lazily on first use, or use a public message topic instead. `AppLifecycleListener` is public, but only `appFrameCreated`, `welcomeScreenDisplayed`, `projectFrameClosed`, `projectOpenFailed`, `appClosing`, and `appWillBeClosed` are safe — `appStarted` and `beforeAppWillBeClosed` are internal.
- **`io.sentry.Sentry.init(SentryOptions)`** — internal in Sentry 8.x. Use the `Sentry.init { options -> ... }` lambda overload, and **configure the instance you are given**; returning a new `SentryOptions` silently drops the config.
- **`com.intellij.openapi.util.SystemInfo`** — `getOsNameAndVersion()`, `OS_NAME`, and `OS_VERSION` are `@ScheduledForRemoval`/`@Obsolete`. Use `com.intellij.util.system.OS.CURRENT` and `OS.CURRENT.version()`.
- **`com.intellij.openapi.application.PathManager`** — mixed. Some members are internal (`getHomePathFor`, `setPathSelector`, `getIndexRoot`, `getDefaultLogPathFor`) and others are obsolete (`getHomePath`, and similar). `getSystemPath()`, `getPluginsPath()`, `getTempPath()`, and `getLogPath()` are clean, so check the individual method rather than the whole class.

## Building and testing

`FlutterXPluginInfo.kt` is generated into `src/main/kotlin/codegen/`. The generator must not delete the file at configuration time — doing so broke every Gradle invocation that did not run the task.

```bash
./gradlew compileKotlin
./gradlew test --tests "SentryReportingServiceTest"
./gradlew buildPlugin
./gradlew verifyPlugin      # before any release
```

## Releasing

Releases are tag-triggered and published to Marketplace by `.github/workflows/release.yml`. Follow `.codex/skills/flutterx-release/SKILL.md`: push the version branch first, then the `v<version>` tag. Never push the tag before the branch exists — the workflow checks out a branch named after the tag.
