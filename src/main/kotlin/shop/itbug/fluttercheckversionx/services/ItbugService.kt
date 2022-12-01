package shop.itbug.fluttercheckversionx.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import shop.itbug.fluttercheckversionx.model.user.User

data class LoginParam(val loginNumber: String, val password: String)

interface ItbugService {

    /**
     * 登录接口
     * @return 成功返回一个接口
     */
    @POST("api/user-public/login")
    fun login(@Body param: LoginParam): Call<JSONResult<String?>>

    /**
     * 加载用户信息接口
     *
     */
    @GET("api/get-user-by-token")
    fun getUserInfo(@Query("token") token: String) : Call<JSONResult<User?>>
}