package shop.itbug.flutterx.constance

import com.intellij.ui.dsl.builder.Panel
import shop.itbug.flutterx.i18n.PluginBundle


const val discordUrl = "https://discord.gg/ethKNxKRcZ"
const val qqGroup = "https://qm.qq.com/q/3zYRrSC7zW"

/// 自建 Sentry 的 DSN（项目 12，经 rustrak.itbug.shop 反向代理，走 HTTPS）。
/// 用户可在 Settings → FlutterX 里修改。
const val SENTRY_DSN = "https://436a5096cc2c4f38b8285d6026541b03@rustrak.itbug.shop/12"

/// 旧版直连 IP:8088 的明文 DSN，升级后自动迁移成 [SENTRY_DSN]。
const val LEGACY_SENTRY_DSN = "http://436a5096cc2c4f38b8285d6026541b03@64.90.30.70:8088/12"


object Links {

    //问题反馈
    const val ISSUES_URL = "https://github.com/mdddj/dd_flutter_idea_plugin/issues"

    //预填内容的 issue 创建页
    const val NEW_ISSUE_URL = "https://github.com/mdddj/dd_flutter_idea_plugin/issues/new"

    const val DOCUMENT_DEFAULT_URL = "https://mdddj.github.io/flutterx-doc/en/"

    const val RIVERPOD = "https://mdddj.github.io/flutterx-doc/en/settings/riverpod/"
    const val OPEN_IN =
        "https://mdddj.github.io/flutterx-doc/en/settings/quick-open-subdirectory/"

    const val DriftDocument = "https://mdddj.github.io/flutterx-doc/en/vm/drift/"

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

    // flutter storage mirror
    const val FLUTTER_STORAGE_BASE_URL_BY_CN = "https://storage.flutter-io.cn"


    //pub服务器地址
    const val PUB_SERVER_URL = "https://pub.dartlang.org"

    //中国区
    const val PUB_CFUG = "https://pub.flutter-io.cn"

    //清华大学 TUNA 协会
    const val QINGHUA_PUB_SERVER_URL = "https://mirrors.tuna.tsinghua.edu.cn/dart-pub"
    const val QINGHUA_FLUTTER_STORAGE_BASE_URL = "https://mirrors.tuna.tsinghua.edu.cn/flutter"

    fun generateDocCommit(link: String): String {
        return "<a href='$link'>${PluginBundle.doc}</a>"
    }

}

fun Panel.documentCommentRow(link: String) {
    row {
        comment(Links.generateDocCommit(link))
    }
}
