# FlutterX Settings UI DSL 2.0 Redesign Draft

## Current Issues

- The basic page mixes global language, Flutter update checking, Dio socket, request list display, assets scan, and copy-key mapping in one long vertical flow.
- The language row uses six radio buttons in one line, which becomes crowded in narrow settings windows and creates a very wide first row.
- The Flutter version section contains enable state, mirror selection, current version, changelog, ignore list, and documentation, but the hierarchy is not clear.
- The Dio section combines server control and display options. The port field and restart button do not visually read as a single server-control row.
- Several dependent options stay visually active even when their parent feature is disabled.
- Advanced options such as copy-key mapping are always visible, increasing scan cost for regular users.

## Proposed Information Architecture

```text
Basic
├─ Language
│  └─ Language: [System v]    Restart IDEA to take effect
│
├─ Flutter SDK Updates
│  ├─ [x] Check for new Flutter versions
│  ├─ Flutter release source: [Use default v]    Visit
│  ├─ Pub server:              [Use default v]    Visit
│  ├─ Current version: 3.44.0   [Changelog]
│  ├─ Ignored versions:         [list]
│  └─ Documentation
│
├─ Dio Socket
│  ├─ [x] Enable FlutterX socket   (?)
│  ├─ Listening port: [9999] [Restart]
│  └─ [x] New API notification
│
├─ Dio Request List
│  ├─ [x] Display domain name       [ ] Display query parameters
│  ├─ [x] Show request method       [x] Display status code
│  ├─ [x] Display request time      [x] Display date
│  ├─ [x] Bold URL                  [x] Display data size
│  └─ Documentation
│
├─ Assets Scan
│  ├─ [x] Enable assets scan
│  ├─ Trigger text:  [assets]
│  ├─ Trigger length:[3]
│  ├─ Scan folder:   [assets]
│  └─ Documentation
│
└─ Advanced
   └─ Copy All Keys
      ├─ URL:                 [url]
      ├─ Method:              [method]
      ├─ Headers:             [headers]
      ├─ Query params:        [query]
      ├─ Body:                [body]
      ├─ Response status code:[responseStatusCode]
      ├─ Response:            [response]
      ├─ Request time:        [requestTime]
      └─ Timestamp:           [timestamp]
```

## Layout Rules

- Use `comboBox` for language selection instead of six radio buttons. This keeps the first group compact and localizes better.
- Keep server control in a dedicated `Dio Socket` group and request display switches in a separate `Dio Request List` group.
- Wrap dependent rows with `indent { ... }.enabledIf(master.selected)` so disabled features look disabled.
- Move copy-key mapping into `collapsibleGroup("Advanced")`, collapsed by default if supported by the current platform version.
- Prefer `row("Label:")` for editable fields so the DSL creates correct label relationships and accessibility metadata.
- Use `columns(COLUMNS_SHORT)` for the port and numeric fields, and `resizableColumn()` only on fields that should expand.

## Kotlin UI DSL 2.0 Draft

```kotlin
fun redesignedBasicSettingsPanel(
    project: Project,
    model: AppStateModel,
    dioSetting: DoxListeningSetting,
    parentDisposable: Disposable,
    onChange: (state: AppStateModel) -> Unit
): DialogPanel {
    val languageList = listOf("System", "中文", "繁體", "English", "한국어", "日本語")

    return panel {
        group(PluginBundle.get("setting.language")) {
            row("${PluginBundle.get("setting.language")}:") {
                comboBox(languageList)
                    .bindItem(
                        { model.lang },
                        { model.lang = it ?: "System" }
                    )
                    .columns(COLUMNS_MEDIUM)
            }.rowComment(PluginBundle.get("setting.reset.tip"))
        }

        group(PluginBundle.get("check_flutter_version_title")) {
            lateinit var checkFlutterVersion: Cell<JBCheckBox>

            row {
                checkFlutterVersion = checkBox(PluginBundle.get("open"))
                    .bindSelected(dioSetting::checkFlutterVersion)
            }.rowComment(PluginBundle.get("check_flutter_version_comment"))

            indent {
                row("Flutter release source:") {
                    PubDevMirrorImageSetting.flutterCheckComboBox(this, dioSetting)
                    browserLink("Visit", dioSetting.checkFlutterVersionUrl)
                }

                row("Pub server:") {
                    PubDevMirrorImageSetting.pubServerComboBox(this, dioSetting)
                    browserLink("Visit", dioSetting.pubServerUrl)
                }

                row("Current Flutter version:") {
                    cell(FlutterVersionCheckPanel(project = project))
                }

                row(PluginBundle.get("ignore_this_version_to_check")) {
                    cell(FlutterVersionIgnoreList())
                        .align(AlignX.FILL)
                        .resizableColumn()
                }.resizableRow()
            }.enabledIf(checkFlutterVersion.selected)

            documentCommentRow(Links.CHECK_FLUTTER_VERSION_DOC_LINK)
        }

        group("Dio Socket") {
            lateinit var enableSocket: Cell<JBCheckBox>

            row {
                enableSocket = checkBox("Enable FlutterX socket")
                    .bindSelected(dioSetting::enableFlutterXDioSocket)
                    .gap(RightGap.SMALL)
                contextHelp(
                    PluginBundle.get("enable_flutterx_socket_setting_contexthelp") + " (Dio, SP, Hive, Log)",
                    "Tips"
                )
            }

            indent {
                row("Listening port:") {
                    intTextField()
                        .bindIntText(
                            { model.serverPort.toInt() },
                            { model.serverPort = it.toString() }
                        )
                        .columns(COLUMNS_SHORT)
                        .gap(RightGap.SMALL)
                        .onChanged { component ->
                            PluginStateService.changeState { it.copy(serverPort = component.text) }
                        }

                    button(PluginBundle.get("reset")) {
                        DioApiService.getInstance().reset(project)
                    }
                }

                row {
                    checkBox(PluginBundle.get("setting.new.tips"))
                        .bindSelected(model::apiInToolwindowTop)
                }
            }.enabledIf(enableSocket.selected)
        }

        group("Dio Request List") {
            twoColumnsRow({
                checkBox(PluginBundle.get("display_domain_name"))
                    .bindSelected(dioSetting::showHost)
            }, {
                checkBox(PluginBundle.get("display.query.parameters"))
                    .bindSelected(dioSetting::showQueryParams)
            })

            twoColumnsRow({
                checkBox(PluginBundle.get("show.request.method"))
                    .bindSelected(dioSetting::showMethod)
            }, {
                checkBox(PluginBundle.get("display.status.code"))
                    .bindSelected(dioSetting::showStatusCode)
            })

            twoColumnsRow({
                checkBox(PluginBundle.get("display.time"))
                    .bindSelected(dioSetting::showTimestamp)
            }, {
                checkBox(PluginBundle.get("time"))
                    .bindSelected(dioSetting::showDate)
            })

            twoColumnsRow({
                checkBox(PluginBundle.get("bold.link"))
                    .bindSelected(dioSetting::urlBold)
            }, {
                checkBox(PluginBundle.get("dio.setting.show.data.size"))
                    .bindSelected(dioSetting::showDataSize)
            })

            row {
                comment(Links.generateDocCommit(Links.DIO))
            }
        }

        group(PluginBundle.get("ass.setting.title")) {
            lateinit var enableAssetsScan: Cell<JBCheckBox>

            row {
                enableAssetsScan = checkBox("Enable assets scan")
                    .bindSelected(model::assetsScanEnable)
            }

            indent {
                row(PluginBundle.get("ass.1")) {
                    textField()
                        .bindText(model::assetCompilationTriggerString)
                        .columns(COLUMNS_MEDIUM)
                }

                row(PluginBundle.get("ass.3")) {
                    intTextField()
                        .bindIntText(model::assetCompilationTriggerLen)
                        .columns(COLUMNS_SHORT)
                }

                row(PluginBundle.get("ass.5")) {
                    textField()
                        .bindText(model::assetScanFolderName)
                        .columns(COLUMNS_MEDIUM)
                }
            }.enabledIf(enableAssetsScan.selected)

            row {
                comment(Links.generateDocCommit(Links.ACCESS_ICON))
            }
        }

        collapsibleGroup("Advanced") {
            group("Copy All Keys") {
                row("URL:") { textField().bindText(dioSetting.copyKeys::url).columns(COLUMNS_MEDIUM) }
                row("Method:") { textField().bindText(dioSetting.copyKeys::method).columns(COLUMNS_MEDIUM) }
                row("Headers:") { textField().bindText(dioSetting.copyKeys::headers).columns(COLUMNS_MEDIUM) }
                row("Query params:") { textField().bindText(dioSetting.copyKeys::queryParams).columns(COLUMNS_MEDIUM) }
                row("Body:") { textField().bindText(dioSetting.copyKeys::body).columns(COLUMNS_MEDIUM) }
                row("Response status code:") { textField().bindText(dioSetting.copyKeys::responseStatusCode).columns(COLUMNS_MEDIUM) }
                row("Response:") { textField().bindText(dioSetting.copyKeys::response).columns(COLUMNS_MEDIUM) }
                row("Request time:") { textField().bindText(dioSetting.copyKeys::requestTime).columns(COLUMNS_MEDIUM) }
                row("Timestamp:") { textField().bindText(dioSetting.copyKeys::timestamp).columns(COLUMNS_MEDIUM) }
                row { comment(Links.generateDocCommit(Links.DIO_IMAGE)) }
            }
        }
    }
}
```

## Optional Extraction

The current `PubDevMirrorImageSetting.createPanel(...)` and `createFlutterCheckUrlPanel(...)` create complete rows. For the redesigned layout, split them into smaller cell-level helpers:

```kotlin
fun pubServerComboBox(row: Row, setting: DoxListeningSetting): Cell<ComboBox<DartPubMirrorImage>>
fun flutterCheckComboBox(row: Row, setting: DoxListeningSetting): Cell<ComboBox<FlutterCheckUrlMirrorImage>>
```

This lets the settings page own labels and links while the mirror helper only owns the combo-box model and binding.
