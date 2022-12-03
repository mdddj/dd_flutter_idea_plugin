package shop.itbug.fluttercheckversionx.services

import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import shop.itbug.fluttercheckversionx.services.Env.Dev
import shop.itbug.fluttercheckversionx.services.Env.Pro
import shop.itbug.fluttercheckversionx.util.CredentialUtil

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
    ///添加请求头
    private val client = OkHttpClient.Builder()
        .addInterceptor { chain ->
            var request = chain.request()
            CredentialUtil.token?.apply {
                request = request.newBuilder().addHeader("Authorization", this).build()
            }
            chain.proceed(request)
        }
        .build()
    private val retrofit =
        Retrofit.Builder()
            .baseUrl(host).addConverterFactory(GsonConverterFactory.create())
            .client(client)
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
