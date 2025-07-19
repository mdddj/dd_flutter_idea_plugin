package shop.itbug.fluttercheckversionx.constance

import com.intellij.ui.dsl.builder.Panel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle


const val discordUrl = "https://discord.gg/ethKNxKRcZ"
const val qqGroup = "https://qm.qq.com/q/3zYRrSC7zW"


object Links {
    const val RIVERPOD = "https://flutterx.itbug.shop/riverpod.html"
    const val OPEN_IN =
        "https://flutterx.itbug.shop/%E5%BF%AB%E9%80%9F%E6%89%93%E5%BC%80%E5%AD%90%E7%9B%AE%E5%BD%95%E6%96%87%E4%BB%B6%E5%A4%B9.html"


    //链接
    const val LINK = "https://flutterx.itbug.shop/links.html"

    //编辑器图标
    const val ICON = "https://flutterx.itbug.shop/%E5%86%85%E8%81%94%E8%B5%84%E4%BA%A7%E6%98%BE%E7%A4%BA.html"

    //资产生成
    const val ASSETS =
        "https://flutterx.itbug.shop/%E8%B5%84%E4%BA%A7%E7%94%9F%E6%88%90%E7%B1%BB%E8%B0%83%E7%94%A8.html"


    //资产预览
    const val ACCESS_ICON = "https://flutterx.itbug.shop/%E8%B5%84%E4%BA%A7%E9%A2%84%E8%A7%88.html"

    // dio
    const val DIO = "https://flutterx.itbug.shop/starter.html"

    const val DIO_IMAGE = "https://flutterx.itbug.shop/%E6%8E%A5%E5%8F%A3%E4%BF%A1%E6%81%AF%E6%88%AA%E5%9B%BE.html"

    // l10n
    const val L10N_DOC = "https://flutterx.itbug.shop/l10n-editor.html"


    //新版本检查
    const val CHECK_FLUTTER_VERSION_DOC_LINK =
        "https://flutterx.itbug.shop/flutter%E6%96%B0%E7%89%88%E6%9C%AC%E6%A3%80%E6%B5%8B.html"


    //flutter版本信息
    const val DEFAULT_FLUTTER_VERSION_INFO_URL =
        "https://storage.googleapis.com/flutter_infra_release/releases/releases_macos.json"

    //中国区镜像
    const val DEFAULT_FLUTTER_VERSION_INFO_URL_BY_CN =
        "https://storage.flutter-io.cn/flutter_infra_release/releases/releases_macos.json"


    //pub服务器地址
    const val PUB_SERVER_URL = "https://pub.dartlang.org"

    //中国区
    const val PUB_CFUG = "https://pub.flutter-io.cn"

    //上海交通大学
    const val SHANGHAI_PUB_SERVER_URL = "https://mirror.sjtu.edu.cn/dart-pub"

    //清华大学 TUNA 协会
    const val QINGHUA_PUB_SERVER_URL = "https://mirrors.tuna.tsinghua.edu.cn/dart-pub"

    fun generateDocCommit(link: String): String {
        return "<a href='$link'>${PluginBundle.doc}</a>"
    }

}

fun Panel.documentCommentRow(link: String) {
    row {
        comment(Links.generateDocCommit(link))
    }
}