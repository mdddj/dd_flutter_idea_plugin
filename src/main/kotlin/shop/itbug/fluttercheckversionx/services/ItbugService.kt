package shop.itbug.fluttercheckversionx.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

data class LoginParam(val loginNumber: String, val password: String)

interface ItbugService {

    /**
     * 登录接口
     * @return 成功返回一个接口
     */
    @POST("api/user-public/login")
    fun login(@Body param: LoginParam): Call<JSONResult<String?>>
}