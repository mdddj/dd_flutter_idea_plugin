package shop.itbug.flutterx.constance

import com.intellij.ui.dsl.builder.Panel
import shop.itbug.flutterx.i18n.PluginBundle


const val discordUrl = "https://discord.gg/ethKNxKRcZ"
const val qqGroup = "https://qm.qq.com/q/3zYRrSC7zW"


object Links {

    const val DOCUMENT_DEFAULT_URL = "https://mdddj.github.io/flutterx-doc/en/"

    const val RIVERPOD = "https://mdddj.github.io/flutterx-doc/en/settings/riverpod/"
    const val OPEN_IN =
        "https://mdddj.github.io/flutterx-doc/en/settings/quick-open-subdirectory/"


    //链接
    const val LINK = "https://mdddj.github.io/flutterx-doc/en/settings/links/"

    //编辑器图标
    const val ICON = "https://mdddj.github.io/flutterx-doc/en/settings/inline-asset-display/"

    //资产生成
    const val ASSETS =
        "https://mdddj.github.io/flutterx-doc/en/assets/asset-generation-class/"


    //资产预览
    const val ACCESS_ICON = "https://mdddj.github.io/flutterx-doc/en/assets/asset-preview/"

    // dio
    const val DIO = "https://mdddj.github.io/flutterx-doc/en/dio/starter/"

    const val DIO_IMAGE = "https://mdddj.github.io/flutterx-doc/en/dio/request-screenshot/"

    // l10n
    const val L10N_DOC = "https://mdddj.github.io/flutterx-doc/en/other/l10n-editor/"


    //新版本检查
    const val CHECK_FLUTTER_VERSION_DOC_LINK =
        "https://mdddj.github.io/flutterx-doc/en/other/flutter-version-detection/"


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