package shop.itbug.fluttercheckversionx.services


import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.util.io.HttpRequests

class FlutterVersionCheckException(message: String) : Exception(message)

object FlutterService {
    fun getVersion(): FlutterVersions {
        try {
            val url = "https://storage.googleapis.com/flutter_infra_release/releases/releases_macos.json"
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


val testRelease = Release(
    "", "3.25.0", "3.5.4", "2024-10-24T17:38:00.719363Z", "", ""
)