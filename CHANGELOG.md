# Changelog

## Unreleased

## [5.0.2] - 2024-11-02

- New: pubspec.yaml add package last update time
- New: pubspec.yaml package document add more details
- Fix known issues
- Supplementary functional documentation

### [5.0.1] - 2024-10-29

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

### [5.0.0] - 2024-10-22

- Fix: dio api copy data to image action if null will null exception
- Fix: If the flutter command does not exist, an error pop-up will
  appear,[(#56)](https://github.com/mdddj/dd_flutter_idea_plugin/issues/56)
- Fix: Fixed the issue of method font misalignment in the Dio api in zoom mode
- New: Set the JSON editor and Dart editor to follow the idea for scaling
- New: Optimize JSON viewer format
- New: Add the function of returning the request body header from the right-click menu of `Dio`
- New: Open the flutter (android,ios,macos) directory and add settings options, (ide setting->flutterx)
- New: Add local asset preview to the editor (image type)

### [4.9.9] - 2024-10-17

- Fix: `flutter_lint: 5.0` `part of` auto-completion action not show
- Fix: `pubspec.yaml` left icon menu `open directory` not working bug
- New: Support monitoring of `Dio` `FormData` API, need upgrade to `dd_check_plugin:3.2.5`

### [4.9.8] - 2024-10-09

- Adaptation android studio 2024.2.1
- Support `k2` mode

### [4.9.7] - 2024-09-21

- Fix:[#54](https://github.com/mdddj/dd_flutter_idea_plugin/issues/54)
- New: Added detection of unused packages in the project.
- Update: Added detection for the bottom toolbar icons; the entry point will not be displayed for non-Flutter projects.

![](https://minio.itbug.shop/blog/simple-file/Snipaste_2024-09-21_11-41-38___1726890133117___.png)

### [4.9.5] - 2024-09-13

- Fix:[#52](https://github.com/mdddj/dd_flutter_idea_plugin/issues/52)
- Fix:[#51](https://github.com/mdddj/dd_flutter_idea_plugin/issues/51)
- New: open with tool
  ![](https://minio.itbug.shop/blog/simple-file/Snipaste_2024-09-13_14-47-33___1726210071283___.png)

### [4.9.4] - 2024-09-05

- 更新: 修改flutter新版本,whats new的跳转链接
- 新增: dio api 接口信息拷贝,支持自定义 key
    * 详见flutterx设置,Copy all key
- 新增: dio api 拷贝为图片,在右键菜单,数据量太大的接口会报内存堆栈溢出,这个问题不解决

<hr/>

- Update: Modified the link for “What’s New” in the new version of Flutter.
- New: Added Dio API interface information copy feature, supporting custom keys.
    * See Flutterx settings for “Copy all key”.
- New: Added Dio API copy as image feature in the right-click menu. Interfaces with too much data will cause memory
  stack overflow, and this issue will not be resolved.

### [4.9.3] - 2024-09-03

- dio拷贝接口信息(Copy all),添加格式化 json

<hr/>

- dio copy interface information (Copy all), add formatted json

### [4.9.2] - 2024-08-13

- 修复 <a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/50'>#50</a>

<hr />

- Fixed <a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/50'>#50</a>

### [4.9.1] - 2024-07-29

- 修复inlay命名导致的part of library无法关闭的问题
- 修改插件idea版本上限 (2024+)
- <hr />
- Fixed the issue where part of the library could not be closed due to inlay naming
- Updated the plugin's IDEA version upper limit (2024.2+)

### [4.9.0] - 2024-07-24

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

### [4.8.0] - 2024-07-08

- fix some bugs by 4.7.0
- Add `json` to `dart macro` generation tool (bate)

### [4.7.0] - 2024-07-03

- Reconstructed the `json to freezed class code gen` tool,
- Optimize the repair logic of `package` new version detection
- Removed some third-party dependencies and reduced the plug-in package to 1M
- If you encounter problems during use, please submit an issue
- This is the last updated version below `2023.*`. If you want to receive subsequent updates, please update your idea to
  version `2024.*` and `above`.

### [4.6.0] - 2024-06-18

- After optimizing the terminal running logic, windows will no longer be created repeatedly.
- Add `part of libarary` autocomplete command
- Optimize the automatic completion popup timing of **_freezed from json_**
- Fix the problem that **_@Freezed_** does not display the freezed function

### [4.5.2] - 2024-05-30

- Optimize dart file inlay detection performance
- Optimize some known bugs

### [4.5.0] - 2024-05-25

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

### [4.2.1] - 2024-05-17

- Adaptation 2024.2
- Fix performance issues caused by iOS privacy scanning window

### [4.2.0] - 2024-05-15

- Fixed the problem of IP type URI being accessed and parsed incorrectly in the dio panel
- Fixed the issue where the <code><b>&</b></code> symbol in parameters in Dio URL is displayed as blank
- Optimize the font display of json editor and dart editor
- Optimize dart document display format (<b><a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/41'>
  #41</a></b>)
- Fixed an error in automatic completion of yaml version
  number(<b><a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/42'>#42</a></b>)
