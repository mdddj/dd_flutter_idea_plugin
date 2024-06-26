package shop.itbug.fluttercheckversionx.socket

import cn.hutool.core.date.DateUtil
import shop.itbug.fluttercheckversionx.form.socket.Request


class ProjectSocketService {
    companion object {
        fun getTestApi(): Request {
            return Request(
                data = "{}",
                method = "GET",
                queryParams = emptyMap(),
                url = "https://itbug.shop:6666/api/test?hello=1&test=true",
                statusCode = 200,
                body = null,
                headers = emptyMap(),
                responseHeaders = emptyMap(),
                timestamp = 300,
                projectName = "Test Request",
                createDate = DateUtil.now(),
            )
        }
    }

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
        val body: Any?,

        ///请求头
        val headers: Map<String, Any>?,

        ///response 请求头
        val responseHeaders: Map<String, Any>?,

        ///请求耗时
        var timestamp: Int?,

        ///项目名称
        var projectName: String = "",

        ///生成成功
        var createDate: String = DateUtil.now(),

        ///扩展label列表
        var extendNotes: List<String> = listOf()
    )

}
