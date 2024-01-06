package shop.itbug.fluttercheckversionx.services

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import shop.itbug.fluttercheckversionx.services.Env.Dev
import shop.itbug.fluttercheckversionx.services.Env.Pro

//当前环境
val currentEnv = Dev

enum class Env {
    //本机测试环境
    Dev,

    //线上环境
    Pro
}

//获取接口服务
val SERVICE: ApiServiceCreate = when (currentEnv) {
    Dev -> LocalhostServiceCreate
    Pro -> ServiceCreateWithMe
}

open class ApiServiceCreate(var host: String) {
    private var gson: Gson = GsonBuilder()
        .setLenient()
        .create()

    ///添加请求头
    private val client = OkHttpClient.Builder().build()
    private val retrofit =
        Retrofit.Builder()
            .baseUrl(host)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    fun <T> create(serverClass: Class<T>): T = retrofit.create(serverClass)
    inline fun <reified T> create(): T = create(T::class.java)

}

object ServiceCreate : ApiServiceCreate(PUBL_API_URL)
object ServiceCreateWithMe : ApiServiceCreate(MY_SERVICE_HOST)
object LocalhostServiceCreate : ApiServiceCreate(LOCAL_HOST_IP)

/**
 * 统一返回结果
 */
data class JSONResult<T>(
    val state: Int,
    val message: String,
    val data: T
)
