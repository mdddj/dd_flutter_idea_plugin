package shop.itbug.fluttercheckversionx.services

import com.alibaba.fastjson2.JSON
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.io.IOException
import java.lang.reflect.Type

class Retrofit2ConverterFactory : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,  //
        annotations: Array<Annotation>,  //
        retrofit: Retrofit
    ): Converter<ResponseBody, Any> {
        return ResponseBodyConverter(type)
    }

    override fun requestBodyConverter(
        type: Type,  //
        parameterAnnotations: Array<Annotation>,  //
        methodAnnotations: Array<Annotation>,  //
        retrofit: Retrofit
    ): Converter<Any, RequestBody> {
        return RequestBodyConverter()
    }


    internal inner class ResponseBodyConverter<T>(private val type: Type) :
        Converter<ResponseBody, T> {
        @Throws(IOException::class)
        override fun convert(value: ResponseBody): T {
            return try {
                JSON.parseObject(
                    value.source().readByteArray(),
                    type,
                )
            } catch (e: Exception) {
                throw IOException("JSON parse error: " + e.message, e)
            } finally {
                value.close()
            }
        }
    }

    internal inner class RequestBodyConverter<T> : Converter<T, RequestBody> {
        override fun convert(value: T): RequestBody {
            return try {
                val content: String = JSON.toJSONString(value)
                content.toRequestBody(MEDIA_TYPE)
            } catch (e: Exception) {
                throw IOException("Could not write JSON: " + e.message, e)
            }
        }
    }

    companion object {
        private val MEDIA_TYPE: MediaType? = "application/json; charset=UTF-8".toMediaTypeOrNull()
        fun create(): Retrofit2ConverterFactory {
            return Retrofit2ConverterFactory()
        }
    }
}