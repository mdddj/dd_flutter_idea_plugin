# Changelog

## Unreleased


## 6.9.1 - 2026-01-26

### ✨ Drift Debugger Optimizations
- **Column Resizing**: Support dynamic column resizing by dragging header borders.
- **CSV Support**: Added options to export table data to CSV and preview CSV content in the editor.
- **Cell Interactions**: Added a context menu for table cells on hover:
  - One-click copy cell value to clipboard.
  - Inline editing of cell values.
  - Open content in a standalone editor (supports Plain Text and JSON).
  - Quick toggle between timestamp and formatted date/time display.
- **Filtering & UI**: 
  - Improved Filter Builder with type-aware operators.
  - Added zebra-striped rows for better table readability.
  - Refined database and table list interfaces.
  - Added a status bar to display operation logs and status.

### 🎨 UI/UX Improvements
- **Donation Support**: Added a WeChat donation popup on the home page.
- **Internationalization**: Improved localization for English, Traditional Chinese (HK), Japanese, and Korean.

### 🐛 Bug Fixes & Technical
- Fixed Drift database component layout issues.
- Fixed MCP (Model Context Protocol) build configuration.
- Improved selection feedback and animations across the tool windows.

## 6.9.0 - 2026-01-21

### ✨ New Features
- **Drift Database Viewer**: Added complete Drift database viewer with real-time data inspection
  - View and manage Drift database tables and data
  - Support for filtering, sorting, and editing data
  - Export database functionality
  - Multi-language support (EN, CN, HK, JA, KO)
- **Kofi Integration**: Added Kofi donation widget support

### 🐛 Bug Fixes
- Fixed `IndexOutOfBoundsException` in Flutter downloader when switching channels
- Fixed `ArrayIndexOutOfBoundsException` in Privacy Scanner when clearing list
- Fixed download location selector not showing when version is auto-selected
- Fixed Drift component split layout not being draggable

### 🌍 Internationalization
- Added complete i18n support for Drift Database Viewer
  - English, Chinese, Traditional Chinese (HK), Japanese, Korean
- All UI components now support multiple languages

### 🎨 UI/UX Improvements
- Drift viewer now has resizable split panels for better workspace management
- Improved Flutter downloader UX with auto-selection of first version
- Enhanced error handling and user feedback

### 🔧 Technical Improvements
- Optimized VM Service extensions
- Improved Gson safety configuration
- Better icon resource management
- Code cleanup and optimization

## 6.8.0 - 2025-12-12

- New features: Shared Preferences panel (Dart Vm Service)
- Fixed an issue where the format of Alibaba Cloud images added by KTS was confusing
- Added a flutter downloader
- Added "This version no longer prompts" option (Flutter new version detection update)
- Optimized some startup logic to not execute in non-flutter projects

## 6.7.3 - 2025-12-05

- Adaptation `AS 2025.2.2`

## 6.7.2 - 2025-11-27

- Fixed known issues (dart vm http monitor)
- Adaptation IDE 2025.3

## 6.7.0 - 2025-11-13

- Adjust the UI of the dart vm http listening panel
- Compatible with the new stable version of flutter (v3.38.0)
- Add a button for quick access to changelog in the settings panel

## 6.6.0 - 2025-11-06

- Add the dio service listening master switch setting
- Modify the dart vm http request list UI (new: table list)

## 6.5.0 - 2025-11-04

- Adds provider panel (dart vm) beta version
- Add `dart pub run build` command run in background action (freezed,foot status bar)
- Add `open in` popup actions in pubspec.yaml with local package item

## 6.4.3 - 2025-10-31

- Adaptation AS otter version (2025.1)

## 6.4.2 - 2025-10-30

- fix: media_kit crashes bugs in debug mode ([#95](https://github.com/mdddj/dd_flutter_idea_plugin/issues/95))

## 6.4.1 - 2025-10-29

- Fixed some known bugs

## 6.4.0 - 2025-10-27

- Added Cupertino Icons and Material Icons preview window

## 6.3.1 - 2025-10-20

- Add some actions to parse local package references (pubspec.yaml,inlay)

## 6.3.0 - 2025-10-17

- New: Dart VM Service tool window, providing real-time insights into your running Flutter application.
- **VM Info**: View detailed information about the Dart Virtual Machine, including memory usage (current/max RSS).
- **HTTP Monitor**: Capture and inspect HTTP requests made by your application.
- **Logging**: View logs sent via `dart:developer`.

## 6.2.0 - 2025-10-13

- Refactor dart vm listener

## 6.1.0 - 2025-10-08

- New version of the search dialog component (dart package)
- Fixed a bug with blank names in some inlay settings
- Add an Alibaba Cloud image shortcut setting inlay (gradle)

## 6.0.7 - 2025-09-26

- Remove http requests without paths (dart vm)

## 6.0.6 - 2025-09-25

- Optimized the Flutter project detection logic

## 6.0.5 - 2025-09-19

- Fix the issue of Inspector hot-reload state synchronization problems
- Added a setting to disable the Dart VM service
- The VM panel has added some document links.

## 6.0.3 - 2025-09-16

- Fix: Switching VM Service is unresponsive (9.16 dart vm)
- Fix: Fixed an issue where HTTP request listening failed after a hot restart
- Fix: Fixed a bug where the http listening UI right-click menu caused a freeze after isolateId was regenerated
- New: dart types support displaying to the left of the variable name, see [document](https://flutterx.itbug.shop/en/dart-file/parameter-type-inline-display/#display-location)
- New: Dart VM Service tool window, providing real-time insights into your running Flutter application.
    *   **VM Info**: View detailed information about the Dart Virtual Machine, including memory usage (current/max RSS).
    *   **HTTP Monitor**: Capture and inspect HTTP requests made by your application.
    *   **Logging**: View logs sent via `dart:developer`.
![](https://github.com/mdddj/dd_flutter_idea_plugin/blob/5.7.4/images/vm-toolwindow/vm1.png?raw=true)
![](https://github.com/mdddj/dd_flutter_idea_plugin/blob/5.7.4/images/vm-toolwindow/vm2.png?raw=true)
![](https://github.com/mdddj/dd_flutter_idea_plugin/blob/5.7.4/images/vm-toolwindow/vm3.png?raw=true)
    
    See [Document](https://flutterx.itbug.shop/zh/vm/dart_vm_panel/)

## 5.7.6 - 2025-08-18

- Change New [Document](https://mdddj.github.io/flutterx-doc/)

## 5.7.5 - 2025-08-11

- Adapted for 2025.2
- Remove the error warning for Flutter version detection API access failure
- Add disabled scan string setting （**l10n module**）

## 5.7.4 - 2025-08-07

- Adapted for 2025.2

## 5.7.3 - 2025-07-26

- add dart vm service code

## 5.7.2 - 2025-07-19

- fix: resource preview panel load in EDT thread error
- Fix known bugs
- ⚠️Suggest upgrading Idea or AS to version 2025.1. Starting from next month, FlutterX will not push updates for
  versions
  below 2024
- [full changelog](https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/CHANGELOG.md)

## 5.7.0 - 2025-07-02

- Add SP panel right menu actions (SP) [see document](https://flutterx.itbug.shop/shared-preferences.html#bo0451_6)
- [full changelog](https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/CHANGELOG.md)

## 5.6.3 - 2025-05-19

- Fix known bugs
- Add Dart String Element Scanner (l10n panel)
- Add "Delete All" in Log panel
- [full changelog](https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/CHANGELOG.md)

## 5.6.2 - 2025-05-08

- Add `dio` restart button (settings)
- Optimize `l10n` editing panel, add preview text option, preview text in tree (l10n panel)
- Add "delete l10n key" action, in right popup menu (l10n panel)
- Add "Rename l10n key" action, in right popup menu (l10n panel)
- [full changelog](https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/CHANGELOG.md)

## 5.6.1 - 2025-05-06

- Unused package logic detection optimization [#78](https://github.com/mdddj/dd_flutter_idea_plugin/issues/78)
- [full changelog](https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/CHANGELOG.md)

## 5.6.0 - 2025-04-28

- Refactoring Log Panel, see [document](https://flutterx.itbug.shop/log.html)
- Fix known bugs
- [Full changelog](https://github.com/mdddj/dd_flutter_idea_plugin/blob/master/CHANGELOG.md)

## 5.5.3 - 2025-04-25

- Fix known bugs

## 5.5.2 - 2025-04-23

- New features:  add run `flutter gen-l10n` command action (l10n)
- New features: Extract string to l10n key,
  see [document](https://flutterx.itbug.shop/l10n-editor.html#extract-l10n-key)
- fix some bugs

## 5.5.0 - 2025-04-19

- fix: Solve the incorrect usage of `Disposer` in settings
- New: flutter `l10n` editor tool, [See Document](https://flutterx.itbug.shop/l10n-editor.html)

## 5.4.3 - 2025-04-14

- fix: `StringIndexOutOfBoundsException`
- Performance optimization

## 5.4.2 - 2025-04-10

- fix: Repair asset image detection logic
- new feature: dio api generate hurl code
- Update: dart assets check level error to warning

## 5.4.1 - 2025-04-08

- fix some bugs

## 5.4.0 - 2025-04-01

- fix some bugs

## 5.3.4 - 2025-03-17

- fix known bugs

## 5.3.3 - 2025-03-13

- New: add assets preview window
- fix: The problem of jitter in asset images exceeding a certain size (document preview)
- fix: assets code generate tool setting errors
- Some other problems solved

## 5.3.2 - 2025-03-05

- New: `flutter android gradle` Migration Tools (android tool)
- Fix: `dio` The problem of listening server not being destroyed (dio tool)

## 5.3.1 - 2025-03-03

- Fix: dart function comment generator tool error (dart comment)
- Fix: dart assets hover document preview bug (with image) (dart assets preview)
- New: Supports the operation of clicking on the asset path to quickly open files（Need to hold ctrl）

## 5.3.0 - 2025-02-28

- New features: Added assets asset path detection mark (dart)
- New features: If a package is marked by the author and stopped updating, it will be marked in the configuration file (
  yaml)
- New features: Added freezed 3.0 class quick repair tool (dart)

## 5.2.4 - 2025-02-26

- New: json to freezed class tool (Supports version 3.0.0)

## 5.2.3 - 2025-02-13

- Replaced flutter version update record link
- Fix the wrong command to open an ios project
- Optimize known issues

## 5.2.2 - 2025-01-17

- Fix: dart type click navigator to define error (2024.3)

## 5.2.1 - 2025-01-11

- Fix: Fixed the problem of version replacement and mouse pointer losing new line at the end
- Fix: Strengthen package search constraints
- New: Select `android` directory and select `Open In` action (and ios directory)
- ![img_1.png](https://minio.itbug.shop/blog/simple-file/img_1___1736561506441___.png)

## 5.2.0 - 2025-01-04

- 💐 Happy New Year, 2025 💐
- Fix known issues
- Adjust the implementation of some functions

## 5.1.0 - 2024-12-16

- New: Statistics of the size of the package occupied on the disk
- Optimize: `Dio api list window` refactor
- Fix: `assets generate tool` initialization issues in some scenarios
- Removed: pubspec.yaml dart package table dialog
- New: The reconstructed pubspec.yaml check logic supports detection of multiple nested folders.
- New: mirror image setting ui
- Refactor: Ignoring packet detection requires defining each file individually

## 5.0.5 - 2024-12-06

- Add api to display in reverse order
- Fix the bug of automatically sliding to the bottom
- Tweaked frozen generator UI
- Update some icons and delete some icons, compress some image assets, and reduce the package size to the extreme, only
  1.5M
- Optimize the component hierarchy of window
- Remove `GlobalScope.launch` to avoid the risk of memory leaks

## 5.0.4 - 2024-11-28

- Fix: autocomplete failure problem (Assets)
- Fix: Bugs caused by lack of release time (dart package)
- New: package autocomplete
- ![1.gif](https://minio.itbug.shop/blog/simple-file/Kapture2024-11-28at15.03.38___1732777496374___.gif)
- New: Add optional configuration of pub.dev image in China region (pub.dev)
- ![2.png](https://minio.itbug.shop/blog/simple-file/img___1732782302206___.png)

## 5.0.3 - 2024-11-26

- Fix: freezed name save error
- Fix: Dio windows memory leak
- Optimize: Faster display of dart type and support cmd click to jump
- Optimize: Optimized dart document markdown render panel

## 5.0.2 - 2024-11-04

- New: pubspec.yaml add package last update time
- New: pubspec.yaml package document add more details
- Fix known issues
- Supplementary functional documentation

## 5.0.1 - 2024-10-29

- Fix: Analyze the issue of inaccurate location after file changes (
  `pubspec.yaml`) [(#59)](https://github.com/mdddj/dd_flutter_idea_plugin/issues/59)
- Adjust the inspection logic of `widget to ConsumerWidget`
- New: Do not analyze files other than `pubspec.yaml`
- New: `pubspec.yaml` New menu for drainage ditch (*Open package api in browser*,*Show Json Data*)
- New: *riverpod widget to ConsumerWidget*: Automatically add import statements
- New: Support local asset image preview
  ![img.png](https://minio.itbug.shop/blog/simple-file/img___1729914650363___.png)
- New: assets image preview
- ![img.png](https://minio.itbug.shop/blog/simple-file/img___1730164998800___.png)

## 5.0.0 - 2024-10-22

- Fix: dio api copy data to image action if null will null exception
- Fix: If the flutter command does not exist, an error pop-up will
  appear,[(#56)](https://github.com/mdddj/dd_flutter_idea_plugin/issues/56)
- Fix: Fixed the issue of method font misalignment in the Dio api in zoom mode
- New: Set the JSON editor and Dart editor to follow the idea for scaling
- New: Optimize JSON viewer format
- New: Add the function of returning the request body header from the right-click menu of `Dio`
- New: Open the flutter (android,ios,macos) directory and add settings options, (ide setting->flutterx)
- New: Add local asset preview to the editor (image type)

## 4.9.9 - 2024-10-17

- Fix: `flutter_lint: 5.0` `part of` auto-completion action not show
- Fix: `pubspec.yaml` left icon menu `open directory` not working bug
- New: Support monitoring of `Dio` `FormData` API, need upgrade to `dd_check_plugin:3.2.5`

## 4.9.8 - 2024-10-09

- Adaptation android studio 2024.2.1
- Support `k2` mode

## 4.9.7 - 2024-09-21

- Fix:[#54](https://github.com/mdddj/dd_flutter_idea_plugin/issues/54)
- New: Added detection of unused packages in the project.
- Update: Added detection for the bottom toolbar icons; the entry point will not be displayed for non-Flutter projects.

## 4.9.5 - 2024-09-13

- Fix:[#52](https://github.com/mdddj/dd_flutter_idea_plugin/issues/52)
- Fix:[#51](https://github.com/mdddj/dd_flutter_idea_plugin/issues/51)
- New: open with tool
  ![](https://minio.itbug.shop/blog/simple-file/Snipaste_2024-09-13_14-47-33___1726210071283___.png)

## 4.9.4 - 2024-09-05

- 更新: 修改flutter新版本,whats new的跳转链接
- 新增: dio api 接口信息拷贝,支持自定义 key
    * 详见flutterx设置,Copy all key
- 新增: dio api 拷贝为图片,在右键菜单,数据量太大的接口会报内存堆栈溢出,这个问题不解决
- Update: Modified the link for “What’s New” in the new version of Flutter.
- New: Added Dio API interface information copy feature, supporting custom keys.
    * See Flutterx settings for “Copy all key”.
- New: Added Dio API copy as image feature in the right-click menu. Interfaces with too much data will cause memory
  stack overflow, and this issue will not be resolved.

## 4.9.3 - 2024-09-03

- dio拷贝接口信息(Copy all),添加格式化 json
- dio copy interface information (Copy all), add formatted json

## 4.9.2 - 2024-08-13

- 修复 <a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/50'>#50</a>
- Fixed <a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/50'>#50</a>

## 4.9.1 - 2024-07-29

- 修复inlay命名导致的part of library无法关闭的问题
- 修改插件idea版本上限 (2024+)
- <hr />
- Fixed the issue where part of the library could not be closed due to inlay naming
- Updated the plugin's IDEA version upper limit (2024.2+)

## 4.9.0 - 2024-07-24

- 修复: json查看器没有格式化的问题
- 修复: Dio接口数据类型int会被转化为double类型的bug
- 修复: pubspec.yaml工具条可能引发的线程安全问题
- 修复: 修复part of 会造成卡顿的bug
- 修复: json转freezed打开后配置没有被重新加载的问题
- 新增: json转dart macro 添加到了Dio 右键菜单
- 新增: part of 新增相对路径自动补全,输入"part"会触发自动联想
- 新增: dio接口到来时添加一个通知,点击可以在浏览器打开链接
- 新增: json转dart macro对象工具 (dart bete)
- 新增: json转freezed添加isar选项
  <hr/>
- Fixed: Issue where the JSON viewer was not formatted
- Fixed: Bug where Dio interface data type int was being converted to double
- Fixed: Thread safety issue potentially caused by the pubspec.yaml toolbar
- Fixed: Bug where part of would cause lag
- Fixed: Issue where JSON to Freezed configuration was not reloaded after opening
- Added: JSON to Dart macro added to the Dio right-click menu
- Added: Auto-completion for relative paths with part of; typing "part" triggers auto-suggestions
- Added: Notification when a Dio interface call arrives, with a clickable link to open in the browser
- Added: JSON to Dart macro object tool (Dart beta)
- Added: Isar option to JSON to Freezed conversion

## 4.8.0 - 2024-07-08

- fix some bugs by 4.7.0
- Add `json` to `dart macro` generation tool (bate)

## 4.7.0 - 2024-07-03

- Reconstructed the `json to freezed class code gen` tool,
- Optimize the repair logic of `package` new version detection
- Removed some third-party dependencies and reduced the plug-in package to 1M
- If you encounter problems during use, please submit an issue
- This is the last updated version below `2023.*`. If you want to receive subsequent updates, please update your idea to
  version `2024.*` and `above`.

## 4.6.0 - 2024-06-18

- After optimizing the terminal running logic, windows will no longer be created repeatedly.
- Add `part of libarary` autocomplete command
- Optimize the automatic completion popup timing of **_freezed from json_**
- Fix the problem that **_@Freezed_** does not display the freezed function

## 4.5.2 - 2024-05-30

- Optimize dart file inlay detection performance
- Optimize some known bugs

## 4.5.0 - 2024-05-25

- Fix the problem of stuck idea or Android studio
- Upgrade kotlin version to `2.0.0` to `support k2 compiler`
- Optimize dart package version replacement logic
- Fix the bug that modifying the file pubspec.yaml may cause unresponsiveness
- Fix the problem of `flutter upgrade` and `flutter pub run build_runner build` command execution failure(idea **241+**)
- New terminal adapted to `IDEA 2024.1+`
- Fix some operation error reports. <code>Slow operations are prohibited on EDT. See
  SlowOperations.assertSlowOperationsAreAllowed javadoc.</code>
- Adaptation 2024.2
- Remove some deprecated functions
- Replace flutter update log `What's New` Url Link

## 4.2.1 - 2024-05-17

- Adaptation 2024.2
- Fix performance issues caused by iOS privacy scanning window

## 4.2.0 - 2024-05-15

- Fixed the problem of IP type URI being accessed and parsed incorrectly in the dio panel
- Fixed the issue where the <code><b>&</b></code> symbol in parameters in Dio URL is displayed as blank
- Optimize the font display of json editor and dart editor
- Optimize dart document display format (<b><a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/41'>
  #41</a></b>)
- Fixed an error in automatic completion of yaml version
  number(<b><a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/42'>#42</a></b>)
