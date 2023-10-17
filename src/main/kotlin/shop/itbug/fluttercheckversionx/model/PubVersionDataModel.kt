package shop.itbug.fluttercheckversionx.model

data class PubVersionDataModel(
    val name: String,
    val latest: Latest,
    val versions: List<Version>
) {
     val lastVersion get() = '^' + latest.version

    /**
     * 判断版本是否为最新版本.将传入的[version]和[latest.version]进行比如,如果不是最新版则执行[apply]函数
     * [apply] 函数回调一个最新版本
     * @return true 已经是最新版本 false 不是最新版本
     */
    fun judge(version: String, apply: (lastVersionString: String) -> Unit) : Boolean {
        if (lastVersion != version) {
            apply.invoke(lastVersion)
            return false
        }
        return true
    }


    /**
     * 获取最新版本的最后更新时间
     *
     */
    private fun getLastUpdateTime(): String {
        var timeString = latest.published
        val toCharArray = timeString.toCharArray()
        val tChat = toCharArray.get(10)
        if (tChat.equals('T')) {
            timeString = timeString.replace("T", " ")
        }
        val dotIndex = timeString.lastIndexOf(".")
        timeString = timeString.substring(0, dotIndex)
        return timeString
    }

    val lastVersionUpdateTimeString get() = getLastUpdateTime()
}

data class Latest(
    val version: String,
    val pubspec: LatestPubspec,
    val archiveURL: String,
    val published: String
)

data class LatestPubspec(
    val name: String,
    val version: String,
    val homepage: String,
    val description: String
)


data class Version(
    val version: String,
    val archiveURL: String,
    val published: String
)
