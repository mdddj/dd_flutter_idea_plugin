package services

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ServiceCreate {

    private val retrofit =
        Retrofit.Builder().baseUrl(PUBL_API_URL).addConverterFactory(GsonConverterFactory.create())
            .build()

    fun <T> create(serverClass: Class<T>): T = retrofit.create(serverClass)

    inline fun <reified T> create(): T = create(T::class.java)

}