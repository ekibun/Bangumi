package soko.ekibun.bangumi.api.smms

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Headers
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import soko.ekibun.bangumi.api.smms.bean.Response
import soko.ekibun.bangumi.util.HttpUtil

interface SmMs{

    @Multipart
    @Headers("user-agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.106 Safari/537.36")
    @POST("/api/upload")
    fun upload(@Part file: MultipartBody.Part): Call<Response>

    companion object {
        private const val SERVER_API = "https://sm.ms"
        fun createInstance(): SmMs{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(SmMs::class.java)
        }
    }
}