# Changelog

## Unreleased

## [4.9.0] - 2024-07-24

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

## [4.8.0] - 2024-07-08

- fix some bugs by 4.7.0
- Add `json` to `dart macro` generation tool (bate)

## [4.7.0] - 2024-07-03

- Reconstructed the `json to freezed class code gen` tool,
- Optimize the repair logic of `package` new version detection
- Removed some third-party dependencies and reduced the plug-in package to 1M
- If you encounter problems during use, please submit an issue
- This is the last updated version below `2023.*`. If you want to receive subsequent updates, please update your idea to
  version `2024.*` and `above`.

## [4.6.0] - 2024-06-18

- After optimizing the terminal running logic, windows will no longer be created repeatedly.
- Add `part of libarary` autocomplete command
- Optimize the automatic completion popup timing of **_freezed from json_**
- Fix the problem that **_@Freezed_** does not display the freezed function

## [4.5.2] - 2024-05-30

- Optimize dart file inlay detection performance
- Optimize some known bugs

## [4.5.0] - 2024-05-25

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

## [4.2.1] - 2024-05-17

- Adaptation 2024.2
- Fix performance issues caused by iOS privacy scanning window

## [4.2.0] - 2024-05-15

- Fixed the problem of IP type URI being accessed and parsed incorrectly in the dio panel
- Fixed the issue where the <code><b>&</b></code> symbol in parameters in Dio URL is displayed as blank
- Optimize the font display of json editor and dart editor
- Optimize dart document display format (<b><a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/41'>
  #41</a></b>)
- Fixed an error in automatic completion of yaml version
  number(<b><a href='https://github.com/mdddj/dd_flutter_idea_plugin/issues/42'>#42</a></b>)
