package shop.itbug.fluttercheckversionx.constance


val helpText = """
# 安装依赖

在你的项目中添加插件依赖
```yaml
dd_check_plugin: any  #或者使用最新版
```

# 初始化

在合适的地方进行初始化,Dio()换成你的自己的dio实例
```Dart
await DdCheckPlugin.instance.init(Dio()); 
```

接入完成.
注意:第一次安装插件需要重启Idea
有问题请加Flutter自学QQ群:__${qqGroup.qqGroupNumber}__

"""