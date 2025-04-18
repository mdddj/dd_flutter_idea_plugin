package shop.itbug.fluttercheckversionx.services


import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.util.io.HttpRequests
import shop.itbug.fluttercheckversionx.config.DioListingUiConfig

class FlutterVersionCheckException(message: String) : Exception(message)

object FlutterService {
    fun getVersion(): FlutterVersions {
        try {
            val url = DioListingUiConfig.setting.checkFlutterVersionUrl
            val get: String = HttpRequests.request(url).readString()
            return Gson().fromJson(get, FlutterVersions::class.java)
        } catch (e: Exception) {
            e.printStackTrace()
            throw FlutterVersionCheckException("Failed to detect new version of flutter:${e.localizedMessage}")
        }
    }
}


///使用channel来判断版本
fun FlutterVersions.getCurrentReleaseByChannel(channel: String): String? {
    return when (channel.trim()) {
        "beta" -> currentRelease.beta
        "stable" -> currentRelease.stable
        "dev" -> currentRelease.dev
        else -> null
    }

}

data class FlutterVersions(
    @SerializedName("base_url")
    var baseURL: String,

    @SerializedName("current_release")
    var currentRelease: CurrentRelease,

    var releases: List<Release>
)

data class CurrentRelease(
    var beta: String,
    var dev: String,
    var stable: String
)

data class Release(
    var hash: String,
    var version: String,
    @SerializedName("dart_sdk_version")
    var dartSDKVersion: String? = null,
    @SerializedName("release_date")
    var releaseDate: String,
    var archive: String,
    var sha256: String
)


