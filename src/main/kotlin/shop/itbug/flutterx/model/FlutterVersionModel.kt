package shop.itbug.flutterx.model

import com.google.gson.annotations.SerializedName

sealed class FlutterLocalVersion{
    data class VersionString(val version: String) : FlutterLocalVersion()
    data class VersionInfo(val versionInfo: FlutterVersionInfo) : FlutterLocalVersion()
}

fun FlutterLocalVersion.getVersionText(): String {
    return when (this) {
        is FlutterLocalVersion.VersionInfo -> versionInfo.flutterVersion
        is FlutterLocalVersion.VersionString -> version
    }.trim()
}

data class FlutterVersionInfo(
    @SerializedName("frameworkVersion")
    val frameworkVersion: String,

    @SerializedName("channel")
    val channel: String,

    @SerializedName("repositoryUrl")
    val repositoryUrl: String,

    @SerializedName("frameworkRevision")
    val frameworkRevision: String,

    @SerializedName("frameworkCommitDate")
    val frameworkCommitDate: String,

    @SerializedName("engineRevision")
    val engineRevision: String,

    @SerializedName("engineCommitDate")
    val engineCommitDate: String,

    @SerializedName("engineContentHash")
    val engineContentHash: String?, // 同样建议设为可空

    @SerializedName("engineBuildDate")
    val engineBuildDate: String?, // 同样建议设为可空

    @SerializedName("dartSdkVersion")
    val dartSdkVersion: String,

    @SerializedName("devToolsVersion")
    val devToolsVersion: String?, // 同样建议设为可空

    @SerializedName("flutterVersion")
    val flutterVersion: String
)