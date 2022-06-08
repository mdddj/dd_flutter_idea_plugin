package shop.itbug.fluttercheckversionx.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

class LoginParam(val loginName: String, val password: String)

interface ItbugService {

    /**
     * 登录接口
     */
    @POST("api/user-public/login")
    suspend fun login(@Body param: LoginParam): JSONResult<String?>
}