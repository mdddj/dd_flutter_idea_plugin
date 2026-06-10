package shop.itbug.flutterx.constance

import shop.itbug.flutterx.constance.Links.DEFAULT_FLUTTER_VERSION_INFO_URL
import shop.itbug.flutterx.constance.Links.DEFAULT_FLUTTER_VERSION_INFO_URL_BY_CN
import shop.itbug.flutterx.i18n.PluginBundle


/// pub.dev镜像站点
enum class DartPubMirrorImage(val title: String, val url: String, val flutterStorageBaseUrl: String? = null) {
    DefaultPub(PluginBundle.defaultText(), Links.PUB_SERVER_URL),
    FlutterChina("Flutter 社区 (CFUG) · ${PluginBundle.get("sugg")}", Links.PUB_CFUG, Links.FLUTTER_STORAGE_BASE_URL_BY_CN),
    QingHuaPub("清华大学TUNA协会", Links.QINGHUA_PUB_SERVER_URL, Links.QINGHUA_FLUTTER_STORAGE_BASE_URL);

    val isChinaMirror: Boolean
        get() = flutterStorageBaseUrl != null

    companion object {
        val chinaMirrors: List<DartPubMirrorImage>
            get() = entries.filter { it.isChinaMirror }
    }

    override fun toString(): String = title
}

///flutter新版本检测站点
enum class FlutterCheckUrlMirrorImage(val title: String, val url: String) {
    DefaultUrl(PluginBundle.defaultText(), DEFAULT_FLUTTER_VERSION_INFO_URL),
    Chine("中国区镜像(存在延迟)", DEFAULT_FLUTTER_VERSION_INFO_URL_BY_CN);

    override fun toString(): String = title
}


val dartKeys = setOf("do", "abstract", "else", "in", "is", "as", "on", "if", "set", "this")
