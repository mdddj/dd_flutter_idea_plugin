package shop.itbug.fluttercheckversionx.model

data class PluginVersion(
    val name: String, // 插件名字
    val currentVersion: String, // 当前使用的版本
    var newVersion: String, // pub里面的最新版本
    val index: Int,  // position 下标
    val startIndex: Int // position 开始位置
)