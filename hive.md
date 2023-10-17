# Hive - Use of toolbars

To use this tool, add `dd_check_plugin` with a version number greater than or equal to `3.0.2`

```yaml
    dd_check_plugin: ^3.0.2
```

# example

```dart

void main() {
  DdCheckPlugin().init(Dio(),
      initHost: '127.0.0.1', //Replace it with your PC's native IP
      port: 9999, //Replace it with your dio listening port, which defaults to 9999
      projectName: "app project name",
      extend: [

        ///Here, add your box list here
        ///It needs to implement the DdPluginHiveBox interface
        HiveToolManager(boxList: [
          DemoHiveBox()
        ])
      ]
  );
}

```

```dart

class DemoHiveBox implements DdPluginHiveBox<String> {


  @override
  String get boxName => 'demo_box';

  @override
  Future<Box<String>> get getBox => Hive.isBoxOpen(boxName) ? Future.value(Hive.box(boxName)) : Hive.openBox(boxName);

}

```

# If you want to automatically display data, please rewrite the toString method

```dart
@freezed
class Logger with _$Logger {
  const factory Logger({
    @JsonKey(name: 'log') @Default('') String log,
  }) = _Logger;

  factory Logger.fromJson(Map<String, dynamic> json) => _$LoggerFromJson(json);


  ///It will appear in the content display text box
  @override
  String toString() {
    return jsonEncode({
      "my-log": log
    });
  }
}


```
