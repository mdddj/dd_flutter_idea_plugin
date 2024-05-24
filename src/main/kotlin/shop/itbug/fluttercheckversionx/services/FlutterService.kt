package shop.itbug.fluttercheckversionx.services


import cn.hutool.http.HttpUtil
import com.alibaba.fastjson2.JSONObject
import com.alibaba.fastjson2.annotation.JSONField

class FlutterVersionCheckException(message: String) : Exception(message)

object FlutterService {


    fun getVersion(): FlutterVersions {
        try {
            val url = " https://storage.googleapis.com/flutter_infra_release/releases/releases_macos.json"
            val get: String = HttpUtil.get(url)
            return JSONObject.parseObject(get, FlutterVersions::class.java)
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
    @JSONField(name = "base_url")
    val baseURL: String,

    @JSONField(name = "current_release")
    val currentRelease: CurrentRelease,

    val releases: List<Release>
)

data class CurrentRelease(
    val beta: String,
    val dev: String,
    val stable: String
)

val testRelease = Release(
    "11", "3.23.9", "3.4.0", "2024-05-17 10:39:16", "1111", "1111"
)

data class Release(
    val hash: String,
    val version: String,
    @JSONField(name = "dart_sdk_version")
    val dartSDKVersion: String? = null,
    @JSONField(name = "release_date")
    val releaseDate: String,
    val archive: String,
    val sha256: String
)


