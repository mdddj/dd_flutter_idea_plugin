package shop.itbug.fluttercheckversionx.hive.model

///hive操作模型
data class HiveActionGetKeys(val action: String = "getKeys", val projectName: String, val boxName: String)
data class HiveActionGetBox(val action: String = "getBoxList", val projectName: String)
data class HiveActionGetValue(
    val action: String = "getValue",
    val projectName: String,
    val key: String,
    val boxName: String
)

