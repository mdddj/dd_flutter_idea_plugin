# Use of dio window

[中文文档(使用谷歌浏览器打开)](https://flutterx.itbug.shop/starter.html)

You can listen to the API requests of the Flutter plug-in dio in this window. There is no need to use the print function
to print the logs related to dio

![image](https://user-images.githubusercontent.com/29020213/216746543-fb9ea063-3250-4d53-b3ef-0aeba89fc871.png)

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
  await DdCheckPlugin().init(Dio(), initHost: '192.168.199.75', port: 9999, conectSuccess: (Socket s) {
    print('Connection succeeded:${s.address}');
  });
}
```

>
> InitHost can automatically recognize your local IP address
>
> ![image](https://user-images.githubusercontent.com/29020213/216746356-58ca9a3b-0df0-41c3-b319-d38945694727.png)

| Attribute                  | Introduce                                          |
|----------------------------|----------------------------------------------------|
| initHost                   | IP address of your development tool computer       |
| port                       | The listening port is generally 9999 by default    |
| conectSuccess              | Connection success callback                        |
| customCoverterResponseData | User-defined parsing of data sent to the idea side |

## Custom listening port

After saving, you need to restart the idea to take effect

![image](https://user-images.githubusercontent.com/29020213/216746563-f8c6522b-7828-488c-953d-8fdbe3f6717d.png)



