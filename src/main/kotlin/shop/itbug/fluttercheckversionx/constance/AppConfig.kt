package shop.itbug.fluttercheckversionx.constance

import shop.itbug.fluttercheckversionx.i18n.PluginBundle

sealed class AppConfig {
    data class QQGroup(val qqGroupNumber: String) : AppConfig()
    data class FlutterIgScanPlugins(val igPlugins: List<String>) : AppConfig()
}


/// pub.dev镜像站点
enum class DartPubMirrorImage(val title: String, val url: String) {
    DefaultPub(PluginBundle.get("default.value"), Links.pubServerUrl),
    FlutterChina("Flutter 社区 (CFUG)", Links.pubCFUG),
    ShanghaiPub("上海交通大学镜像组", Links.shangHaiPubServerUrl),
    QingHuaPub("清华大学TUNA协会", Links.qingHuaPubServerUrl)

}


var igFlutterPlugin = AppConfig.FlutterIgScanPlugins(
    listOf(
        "flutter",
        "flutter_localizations",
        "flutter_test",
        "path"
    )
)//需要忽略扫描的插件,后面要做成可自定义配置的

val dartKeys = setOf("do", "abstract", "else", "in", "is", "as", "on", "if", "set", "this")