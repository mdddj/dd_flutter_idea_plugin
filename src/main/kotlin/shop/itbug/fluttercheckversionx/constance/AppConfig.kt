package shop.itbug.fluttercheckversionx.constance

import shop.itbug.fluttercheckversionx.constance.Links.defaultFlutterInfosUrl
import shop.itbug.fluttercheckversionx.constance.Links.defaultFlutterInfosUrlByCN
import shop.itbug.fluttercheckversionx.i18n.PluginBundle


/// pub.dev镜像站点
enum class DartPubMirrorImage(val title: String, val url: String) {
    DefaultPub(PluginBundle.defaultText(), Links.pubServerUrl),
    FlutterChina("Flutter 社区 (CFUG)", Links.pubCFUG),
    ShanghaiPub("上海交通大学镜像组", Links.shangHaiPubServerUrl),
    QingHuaPub("清华大学TUNA协会", Links.qingHuaPubServerUrl);

    override fun toString(): String = title
}

///flutter新版本检测站点
enum class FlutterCheckUrlMirrorImage(val title: String, val url: String) {
    DefaultUrl(PluginBundle.defaultText(), defaultFlutterInfosUrl),
    Chine("中国区镜像(存在延迟)", defaultFlutterInfosUrlByCN);

    override fun toString(): String = title
}


val dartKeys = setOf("do", "abstract", "else", "in", "is", "as", "on", "if", "set", "this")

