package shop.itbug.fluttercheckversionx.services


import cn.hutool.http.HttpUtil
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

object FlutterService {
    fun getVersion() : FlutterVersions? {
        return try{
            val url = " https://storage.googleapis.com/flutter_infra_release/releases/releases_macos.json"
            val get:String = HttpUtil.get(url)
            val json =  Json { allowStructuredMapKeys = true }
            json.decodeFromString(FlutterVersions.serializer(),get)
        }catch (e: Exception){
            null
        }
    }
}

// To parse the JSON, install kotlin's serialization plugin and do:
//
// val json            = Json { allowStructuredMapKeys = true }
// val flutterVersions = json.parse(FlutterVersions.serializer(), jsonString)


@Serializable
data class FlutterVersions (
    @SerialName("base_url")
    val baseURL: String,

    @SerialName("current_release")
    val currentRelease: CurrentRelease,

    val releases: List<Release>
)

@Serializable
data class CurrentRelease (
    val beta: String,
    val dev: String,
    val stable: String
)

@Serializable
data class Release (
    val hash: String,
    val channel: Channel,
    val version: String,

    @SerialName("dart_sdk_version")
    val dartSDKVersion: String? = null,

    @SerialName("dart_sdk_arch")
    val dartSDKArch: DartSDKArch? = null,

    @SerialName("release_date")
    val releaseDate: String,

    val archive: String,
    val sha256: String
)

@Serializable
enum class Channel(val value: String) {
    @SerialName("beta") Beta("beta"),
    @SerialName("dev") Dev("dev"),
    @SerialName("stable") Stable("stable");
}

@Serializable
enum class DartSDKArch(val value: String) {
    @SerialName("arm64") Arm64("arm64"),
    @SerialName("x64") X64("x64");
}
