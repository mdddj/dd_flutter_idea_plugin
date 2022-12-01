package shop.itbug.fluttercheckversionx.services.event

import com.intellij.util.messages.Topic
import shop.itbug.fluttercheckversionx.model.user.User

interface UserLoginStatusEvent {

    /**
     * 退出登录请设置为null
     */
    fun loginSuccess(user: User?)

    companion object {
        val TOPIC = Topic.create("dd-user-login-status-change",UserLoginStatusEvent::class.java)
    }
}