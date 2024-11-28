package shop.itbug.fluttercheckversionx.model

data class PubSearchResult(
    val packages: List<Package>,
    val next: String
)

data class Package(
    val `package`: String
)


fun Package.psiElementString(): String = "${`package`}: any"


///插件评分
data class PubPackageScore(val likeCount: Int)

data class PubPackageInfo(val score: PubPackageScore, val model: PubVersionDataModel)