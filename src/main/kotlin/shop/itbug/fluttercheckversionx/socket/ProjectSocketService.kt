package shop.itbug.fluttercheckversionx.socket

class ProjectSocketService {

    /**
     * 解析flutter发送过来的模型
     */
    data class SocketResponseModel(
        ///服务器返回谁
        val data: Any?,

        ///请求类型
        val method: String?,

        /// get 查询参数
        val queryParams: Map<String, Any>?,

        ///请求URL
        val url: String?,

        ///状态码
        val statusCode: Int?,

        ///post参数
        val body: Any,

        ///请求头
        val headers: Map<String, Any>?,

        ///response 请求头
        val responseHeaders: Map<String, Any>?,

        ///请求耗时
        var timestamp: Int?,

        ///项目名称
        var projectName: String?
    )

}
