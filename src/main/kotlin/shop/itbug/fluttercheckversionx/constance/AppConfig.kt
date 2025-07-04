package shop.itbug.fluttercheckversionx.constance

import shop.itbug.fluttercheckversionx.constance.Links.DEFAULT_FLUTTER_VERSION_INFO_URL
import shop.itbug.fluttercheckversionx.constance.Links.DEFAULT_FLUTTER_VERSION_INFO_URL_BY_CN
import shop.itbug.fluttercheckversionx.i18n.PluginBundle


/// pub.dev镜像站点
enum class DartPubMirrorImage(val title: String, val url: String) {
    DefaultPub(PluginBundle.defaultText(), Links.PUB_SERVER_URL),
    FlutterChina("Flutter 社区 (CFUG)", Links.PUB_CFUG),
    ShanghaiPub("上海交通大学镜像组", Links.SHANGHAI_PUB_SERVER_URL),
    QingHuaPub("清华大学TUNA协会", Links.QINGHUA_PUB_SERVER_URL);

    override fun toString(): String = title
}

///flutter新版本检测站点
enum class FlutterCheckUrlMirrorImage(val title: String, val url: String) {
    DefaultUrl(PluginBundle.defaultText(), DEFAULT_FLUTTER_VERSION_INFO_URL),
    Chine("中国区镜像(存在延迟)", DEFAULT_FLUTTER_VERSION_INFO_URL_BY_CN);

    override fun toString(): String = title
}


val dartKeys = setOf("do", "abstract", "else", "in", "is", "as", "on", "if", "set", "this")

