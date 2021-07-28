package model

data class PubVersionDataModel (
    val name: String,
    val latest: Latest,
    val versions: List<Version>
)

data class Latest (
    val version: String,
    val pubspec: LatestPubspec,
    val archiveURL: String,
    val published: String
)

data class LatestPubspec (
    val name: String,
    val version: String,
    val homepage: String,
    val description: String
)


data class Version (
    val version: String,
    val archiveURL: String,
    val published: String
)
