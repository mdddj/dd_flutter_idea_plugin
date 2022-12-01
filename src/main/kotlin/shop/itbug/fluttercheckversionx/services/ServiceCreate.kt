package shop.itbug.fluttercheckversionx.services

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import shop.itbug.fluttercheckversionx.services.Env.*

//当前环境
 val currentEnv = Dev
enum class Env{
    Dev,Pro
}

val SERVICE : ApiServiceCreate = when(currentEnv){
    Dev -> LocalhostServiceCreate
    Pro -> ServiceCreateWithMe
}

open class ApiServiceCreate(host: String) {
    private val retrofit =
        Retrofit.Builder().baseUrl(host).addConverterFactory(GsonConverterFactory.create())
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
data class JSONResult<T> (
    val state: Int,
    val message: String,
    val data: T
)
