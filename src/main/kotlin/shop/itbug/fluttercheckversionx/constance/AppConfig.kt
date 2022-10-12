package shop.itbug.fluttercheckversionx.constance

sealed class AppConfig {
    data class QQGroup(val qqGroupNumber: String): AppConfig()
    data class FlutterIgScanPlugins( val igPlugins: List<String>):AppConfig()
}

val qqGroup = AppConfig.QQGroup("706438100")//QQ群号
var igFlutterPlugin = AppConfig.FlutterIgScanPlugins(listOf("flutter","flutter_localizations","flutter_test","path"))//需要忽略扫描的插件,后面要做成可自定义配置的