package shop.itbug.fluttercheckversionx.model

import com.google.gson.annotations.SerializedName
import com.intellij.openapi.util.text.HtmlChunk
import shop.itbug.fluttercheckversionx.util.*


private fun handleCaret(v: String?): String? {
    if (v == null) {
        return null
    }
    return if (v.startsWith("^")) {
        v // 如果以^开头，则原样返回
    } else {
        "^$v" // 如果不是，则在最前面加上^返回
    }
}

///判断一个插件是否有新的版本
/// @return true 有新版本
fun PubVersionDataModel.hasNewVersion(model: DartPluginVersionName): Boolean {
    return when (model.versionType) {
        DartVersionType.Dev -> lastDevVersion?.finalVersionText?.equals(model.finalVersionText)?.not() == true
        DartVersionType.Beta -> lastBetaVersion?.finalVersionText?.equals(model.finalVersionText)?.not() == true
        DartVersionType.Base -> (latest.version.removePrefix("^") == model.finalVersionText).not()
        DartVersionType.Any -> false
    }
}


///返回最新的一个版本号,带^号
fun PubVersionDataModel.getLastVersionText(model: DartPluginVersionName): String? {
    if (!hasNewVersion(model)) {
        return null
    }
    val v = when (model.versionType) {
        DartVersionType.Dev -> lastDevVersion?.finalVersionText
        DartVersionType.Beta -> lastBetaVersion?.finalVersionText
        DartVersionType.Base -> latest.version.removePrefix("^")
        DartVersionType.Any -> null
    }
    return handleCaret(v)
}


data class PubVersionDataModel(
    val name: String, val latest: Latest, val versions: List<Version>, val jsonText: String
) {

    /**
     * 获取最新版本的最后更新时间
     *
     */
    private fun getLastUpdateTime(): String {
        return DateUtils.parseDate(latest.getPublishedString())
    }

    fun formatTime(): String {
        return DateUtils.timeAgo(lastVersionUpdateTimeString)
    }

    val lastVersionUpdateTimeString get() = getLastUpdateTime()
}

data class Latest(
    val version: String,
    val pubspec: Pubspec,
    @SerializedName("archive_url") val archiveURL: String,
    private var published: String? = null
) {
    override fun toString(): String {
        return version
    }

    fun getPublishedString(): String {
        published ?: return ""
        return published ?: ""
    }
}

data class Pubspec(
    val name: String,
    val version: String,
    val homepage: String?,
    val description: String,
    val environment: Any?,
    val dependencies: Any?,
    @SerializedName("dev_dependencies")
    val devDependencies: Any?,
) {
    fun generateDependenciesHtml(deps: List<String>): String {
        val html = HtmlChunk.div()
        val child = mutableListOf<HtmlChunk>()
        for (dependency in deps) {
            child.add(
                HtmlChunk.tag("a").attr("href", "https://pub.dev/packages/${dependency}").addText(dependency)
                    .italic()
            )
            child.add(HtmlChunk.nbsp(2))
        }
        val htmlText = html.children(child).toString()
        return htmlText
    }
}

val Pubspec.filteredDependenciesString: List<String>
    get() {
        if (dependencies is Map<*, *>) {
            return dependencies.keys.filterIsInstance<String>().filter { it != "flutter" }.toList()
        }
        return emptyList()
    }

val Pubspec.filteredDevDependenciesString: List<String>
    get() {
        if (devDependencies is Map<*, *>) {
            return devDependencies.keys.filterIsInstance<String>().toList()
        }
        return emptyList()
    }

val Pubspec.dartPluginModel get() = DartPluginVersionName(name, version)

data class Version(
    val version: String, val published: String, val pubspec: Pubspec
)

val Version.finalVersionText get() = version.removePrefix("^")
val Version.dartPluginModel get() = pubspec.dartPluginModel

val PubVersionDataModel.devVersionList: List<Version> get() = versions.filter { it.dartPluginModel.isDev() }
val PubVersionDataModel.betaVersionList: List<Version> get() = versions.filter { it.dartPluginModel.isBeta() }

//最新的dev版本
val PubVersionDataModel.lastDevVersion get() = devVersionList.lastOrNull()

//最新的beta版本
val PubVersionDataModel.lastBetaVersion get() = betaVersionList.lastOrNull()


