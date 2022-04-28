package shop.itbug.fluttercheckversionx.socket


// socket 连接flutter项目服务类
class ProjectSocketService  {



    /**
     * 解析flutter发送过来的模型
     */
    data class SocketResponseModel(
        val data: Any?,
        val methed: String,
        val queryParams: Map<String, Any>,
        val url: String,
        val statusCode: Int,
        val body: Any,
        val headers: Map<String, Any>,
        val responseHeaders: Map<String, Any>,
        var timesatamp: Int,
        var projectName:String
    )

}
