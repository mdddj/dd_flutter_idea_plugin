package shop.itbug.fluttercheckversionx.constance

import com.intellij.ui.dsl.builder.Panel
import shop.itbug.fluttercheckversionx.i18n.PluginBundle


const val discordUrl = "https://discord.gg/ethKNxKRcZ"
const val qqGroup = "https://qm.qq.com/q/3zYRrSC7zW"


object Links {
    const val riverpod = "https://flutterx.itbug.shop/riverpod.html"
    const val openIn =
        "https://flutterx.itbug.shop/%E5%BF%AB%E9%80%9F%E6%89%93%E5%BC%80%E5%AD%90%E7%9B%AE%E5%BD%95%E6%96%87%E4%BB%B6%E5%A4%B9.html"


    //链接
    const val link = "https://flutterx.itbug.shop/links.html"

    //编辑器图标
    const val icons = "https://flutterx.itbug.shop/%E5%86%85%E8%81%94%E8%B5%84%E4%BA%A7%E6%98%BE%E7%A4%BA.html"

    //资产生成
    const val assets =
        "https://flutterx.itbug.shop/%E8%B5%84%E4%BA%A7%E7%94%9F%E6%88%90%E7%B1%BB%E8%B0%83%E7%94%A8.html"


    //资产预览
    const val accetsIcons = "https://flutterx.itbug.shop/%E8%B5%84%E4%BA%A7%E9%A2%84%E8%A7%88.html"

    // dio
    const val dio = "https://flutterx.itbug.shop/starter.html"

    const val dioImage = "https://flutterx.itbug.shop/%E6%8E%A5%E5%8F%A3%E4%BF%A1%E6%81%AF%E6%88%AA%E5%9B%BE.html"

    //新版本检查
    const val checkFlutterVersion =
        "https://flutterx.itbug.shop/flutter%E6%96%B0%E7%89%88%E6%9C%AC%E6%A3%80%E6%B5%8B.html"

    fun generateDocCommit(link: String): String {
        return "<a href='$link'>${PluginBundle.doc}</a>"
    }
}

fun Panel.documentCommentRow(link: String) {
    row {
        comment(Links.generateDocCommit(link))
    }
}