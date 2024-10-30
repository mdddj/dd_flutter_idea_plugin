package shop.itbug.fluttercheckversionx.constance

import shop.itbug.fluttercheckversionx.i18n.PluginBundle


const val discordUrl = "https://discord.gg/ethKNxKRcZ"
const val qqGroup = "https://qm.qq.com/q/3zYRrSC7zW"


object Links {
    const val riverpod = "https://flutterx.itbug.shop/riverpod.html"
    const val openIn =
        "https://flutterx.itbug.shop/%E5%BF%AB%E9%80%9F%E6%89%93%E5%BC%80%E5%AD%90%E7%9B%AE%E5%BD%95%E6%96%87%E4%BB%B6%E5%A4%B9.html"

    const val link = "https://flutterx.itbug.shop/links.html"
    const val icons = "https://flutterx.itbug.shop/%E5%86%85%E8%81%94%E8%B5%84%E4%BA%A7%E6%98%BE%E7%A4%BA.html"

    fun generateDocCommit(link: String): String {
        return "<a href='$link'>${PluginBundle.doc}</a>"
    }
}