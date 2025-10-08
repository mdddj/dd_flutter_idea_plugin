package shop.itbug.fluttercheckversionx.model

typealias DartPackageItem = Package
data class PubSearchResult(
    val packages: List<Package>,
    val next: String
)

data class Package(
    val `package`: String
)


///插件评分
data class PubPackageScore(val likeCount: Int,val downloadCount30Days: Int)

data class PubPackageInfo(val score: PubPackageScore, val model: PubVersionDataModel)