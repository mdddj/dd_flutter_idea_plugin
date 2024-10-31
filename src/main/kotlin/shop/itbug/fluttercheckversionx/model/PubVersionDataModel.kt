package shop.itbug.fluttercheckversionx.model

import kotlinx.serialization.SerialName
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
        DartVersionType.Dev -> lastDevVersion?.finalVersionText?.equals(model.finalVersionText)?.not() ?: false
        DartVersionType.Beta -> lastBetaVersion?.finalVersionText?.equals(model.finalVersionText)?.not() ?: false
        DartVersionType.Base -> (latest.version.removePrefix("^") == model.finalVersionText).not()
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
        return DateUtils.parseDate(latest.published)
    }

    val lastVersionUpdateTimeString get() = getLastUpdateTime()
}

data class Latest(
    val version: String,
    val pubspec: Pubspec,
    @SerialName("archive_url") val archiveURL: String,
    val published: String
) {
    override fun toString(): String {
        return version
    }
}

data class Pubspec(
    val name: String, val version: String, val homepage: String?, val description: String
)

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


