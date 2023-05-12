package shop.itbug.fluttercheckversionx.services

import retrofit2.Call
import retrofit2.http.*
import shop.itbug.fluttercheckversionx.model.BlogCategory
import shop.itbug.fluttercheckversionx.model.Pageable
import shop.itbug.fluttercheckversionx.model.chat.IdeaMessage
import shop.itbug.fluttercheckversionx.model.chat.SendTextModel
import shop.itbug.fluttercheckversionx.model.resource.MyResource
import shop.itbug.fluttercheckversionx.model.resource.ResourceCategory
import shop.itbug.fluttercheckversionx.model.user.User
import shop.itbug.fluttercheckversionx.services.params.AddCityApiModel
import shop.itbug.fluttercheckversionx.services.params.AddJobParams

data class LoginParam(val loginNumber: String, val password: String)
data class LoginResult(val token: String, val user: User)
interface ItbugService {

    /**
     * 登录接口
     * @return 成功返回一个接口
     */
    @POST("api/user-public/login")
    fun login(@Body param: LoginParam): Call<JSONResult<LoginResult?>>

    /**
     * 加载用户信息接口
     *
     */
    @GET("api/get-user-by-token")
    fun getUserInfo(@Query("token") token: String): Call<JSONResult<User?>>

    /**
     * 查询资源分类列表
     */
    @GET("api/rc/findByType")
    fun getResourceCategorys(@Query("type") type: String): Call<JSONResult<List<ResourceCategory>>>

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
    fun findRoomHistory(
        @Query("roomId") roomId: Int,
        @Query("page") page: Int,
        @Query("pageSize") pageSize: Int
    ): Call<JSONResult<Pageable<IdeaMessage>>>


    /**
     * 添加城市
     */
    @POST("api/admin/jobs/add-city")
    fun addNewJobsCity(@Body params: AddCityApiModel): Call<JSONResult<Any>>

    /**
     * 查询所有城市
     */
    @GET("api/public/jobs/city")
    fun findAllJobCity(): Call<JSONResult<List<ResourceCategory>>>

    /**
     * 发布职位
     */
    @POST("api/public/jobs/add-job")
    fun addJob(@Body params: AddJobParams): Call<JSONResult<Any>>


    /**
     * 查询职位
     */
    @GET("api/public/jobs/find-job")
    fun findAllJob(@QueryMap params: MutableMap<String, Any>): Call<JSONResult<Pageable<MyResource>>>

    /**
     * 获取所有的博客分类
     */
    @GET("api/blog/category-list")
    fun findAllBlogCategory(): Call<JSONResult<List<BlogCategory>>>
}