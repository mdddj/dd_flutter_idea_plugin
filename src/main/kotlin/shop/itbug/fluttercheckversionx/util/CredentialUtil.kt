package shop.itbug.fluttercheckversionx.util

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import shop.itbug.fluttercheckversionx.model.UserAccount

val ca = CredentialAttributes(generateServiceName("ldd", "user"))
val tokenCa = CredentialAttributes(generateServiceName("ldd", "token"))
val openAiCa = CredentialAttributes(generateServiceName("openai", "apiKey"))

///密码存储相关工具类
object CredentialUtil {

    /**
     * 安全存储用户名和密码等信息
     */
    fun saveUserPasswordAndAccount(account: String, password: String) {
        val credent = Credentials(user = account, password = password)
        PasswordSafe.instance.set(ca, credent)
    }

    /**
     * 获取本机存储的用户名和密码
     */
    fun getSavedCredentials(): UserAccount? {
        val get = PasswordSafe.instance.get(ca)
        get?.let {
            val passwordAsString = it.getPasswordAsString()
            val userName = it.userName
            if (passwordAsString != null && userName != null) {
                return UserAccount(userName, passwordAsString)
            }
        }
        return null
    }

    /**
     * 删除本机存储的用户名和密码
     */
    fun removeUserAccount() {
        PasswordSafe.instance.set(ca, null)
    }


    /**
     * 设置openAi的key
     */
    fun setOpenAiKey(apiKey: String) {
        PasswordSafe.instance.setPassword(openAiCa, apiKey)
    }

    /**
     * 读取openAi的key
     */
    val openApiKey: String get() = PasswordSafe.instance.getPassword(openAiCa) ?: ""


    /**
     * 保存一个token
     */
    fun saveToken(token: String) {
        PasswordSafe.instance.setPassword(tokenCa, token)
    }

    /**
     * 读取本机token
     */
    val token: String? get() = PasswordSafe.instance.get(tokenCa)?.getPasswordAsString()

    /**
     * 删除本机token
     */
    fun removeToken() {
        PasswordSafe.instance.set(tokenCa, null)
    }
}
