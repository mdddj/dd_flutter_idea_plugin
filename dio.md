# Use of dio window

You can listen to the API requests of the Flutter plug-in dio in this window. There is no need to use the print function
to print the logs related to dio

## 1.Installation dependency

Add a dependency in the `pubspec. yaml` under the root directory of your Flutter Project

```yaml
dd_check_plugin: ^laset_version
```

run

```bash
flutter pub get
```

## 2.Add code

Connect the FlutterVersionCheckX plug-in at the entrance of your main function

Like this

```dart
void main() {
  await DdCheckPlugin.instance
      .init(Dio(), initHost: '192.168.199.75', port: 9999, conectSuccess: (Socket s) {
    print('Connection succeeded:${s.address}');
  });
}
```

| Attribute                  | Introduce                                          |
|----------------------------|----------------------------------------------------|
| initHost                   | IP address of your development tool computer       |
| port                       | The listening port is generally 9999 by default    |
| conectSuccess              | Connection success callback                        |
| customCoverterResponseData | User-defined parsing of data sent to the idea side |


