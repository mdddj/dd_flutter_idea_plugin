package shop.itbug.fluttercheckversionx.model


data class TextModelResult (
    val data: TextModel?,
    val message: String,
    val state: Long
)

data class TextModel (
    val context: String,
    val id: Long,
    val intro: String,
    val isEncryptionText: Boolean,
    val name: String,
    val viewPassword: String
)
