package shop.itbug.fluttercheckversionx.model

data class PubSearchResult (
    val packages: List<Package>,
    val next: String
)

data class Package (
    val `package`: String
)
