package services

import com.google.gson.JsonObject
import model.PubVersionDataModel
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import java.lang.RuntimeException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * PUB Api 开放接口
 */
const val PUBL_API_URL = "https://pub.dartlang.org/api/"

/**
 * 访问pub开放Api接口
 * 接口url: [https://pub.dartlang.org/api/packages/插件名字]
 */
interface PubService {

    /**
     * 获取插件的相关数据
     */
    @GET("packages/{plugName}")
    fun callPluginDetails(@Path("plugName") plugName:String) : Call<PubVersionDataModel>
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
                else continuation.resumeWithException(RuntimeException("response body is null"))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                continuation.resumeWithException(t)
            }
        })
    }
}