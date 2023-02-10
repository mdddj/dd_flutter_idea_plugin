package shop.itbug.fluttercheckversionx.services

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import shop.itbug.fluttercheckversionx.model.Pageable
import shop.itbug.fluttercheckversionx.model.chat.IdeaMessage
import shop.itbug.fluttercheckversionx.model.chat.SendTextModel
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategory
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.services.params.AddCityApiModel

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

    /**
     * 查询资源分类列表
     */
    @GET("api/rc/findByType")
    fun getResourceCategorys(@Query("type") type: String) : Call<JSONResult<List<ResourceCategory>>>

    /**
     * 发送一条简单的聊天信息
     */
    @POST("ws/send/simple")
    fun sendSimpleMessage(@Body model: SendTextModel): Call<JSONResult<Any>>

    /**
     * 查询房间的聊天历史记录
     * @param [roomId] 房间ID
     */
    @GET("idea-chat/history")
    fun findRoomHistory(@Query("roomId") roomId: Int,@Query("page") page: Int,@Query("pageSize") pageSize: Int): Call<JSONResult<Pageable<IdeaMessage>>>


    /**
     * 添加城市
     */
    @POST("api/admin/jobs/add-city")
    fun addNewJobsCity(@Body params: AddCityApiModel): Call<JSONResult<Any>>
}