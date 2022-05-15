package shop.itbug.fluttercheckversionx.services

import PluginVersionModel
import shop.itbug.fluttercheckversionx.model.PubVersionDataModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import shop.itbug.fluttercheckversionx.model.PubSearchResult
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * PUB Api 开放接口
 */
const val PUBL_API_URL = "https://pub.dartlang.org/"

/**
 * 访问pub开放Api接口
 * 接口url: [https://pub.dartlang.org/api/packages/插件名字]
 */
interface PubService {

    /**
     * 获取插件的相关数据
     */
    @GET("api/packages/{plugName}")
    fun callPluginDetails(@Path("plugName") plugName:String) : Call<PubVersionDataModel>

    /**
     * 获取包的版本列表
     */
    @GET("packages/{pluginName}.json")
    fun getPackageVersions(@Path("pluginName") pluginName: String) : Call<PluginVersionModel>

    /**
     * 搜索包
     */
    @GET("api/search")
    fun search(@Query("q") pluginName: String): Call<PubSearchResult>
}

/**
 * 请求封装挂起
 */
suspend fun <T> Call<T>.await(): T {
    return suspendCoroutine { continuation ->
        enqueue(object : Callback<T>{
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val body = response.body()
                if(body!=null) continuation.resume(body)
                else continuation.resumeWithException(MyException("请求数据出现错误:${response.isSuccessful} $response"))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
}


class MyException(msg: String) : Exception(msg)