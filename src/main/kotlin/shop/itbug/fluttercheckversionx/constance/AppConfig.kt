package shop.itbug.fluttercheckversionx.constance

sealed class AppConfig {
    data class QQGroup(val qqGroupNumber: String): AppConfig()
}

val qqGroup = AppConfig.QQGroup("706438100")//QQ群号