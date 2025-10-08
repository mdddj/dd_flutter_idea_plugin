package shop.itbug.fluttercheckversionx.model

///插件的类型
enum class FlutterPluginType(val type: String, val title: String) {
    Dependencies("dependencies", "Dependencies"),
    DevDependencies("dev_dependencies", "Dev"),
    OverridesDependencies("dependency_overrides", "Overrides")
}

