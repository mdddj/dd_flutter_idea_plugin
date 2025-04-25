package shop.itbug.flutterx.ai.flutterxai.model

import com.intellij.openapi.components.BaseState

interface AIModel {
    fun getName(): String
    fun getProvider(): String
    fun getHost(): String
}


class SiliConCloudAI : AIModel {
    override fun getName(): String {
        return "硅基流动"
    }

    override fun getProvider(): String {
        return "SiliConCloud"
    }

    override fun getHost(): String {
        return "https://api.siliconflow.cn/v1"
    }

}