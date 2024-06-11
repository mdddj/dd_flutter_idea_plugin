# Changelog

## Unreleased

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
