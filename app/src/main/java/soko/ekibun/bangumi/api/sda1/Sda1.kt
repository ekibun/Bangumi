package soko.ekibun.bangumi.api.sda1

import android.util.Log
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.sda1.bean.Response

/**
 * 流浪图库
 */
interface Sda1 {

    /**
     * 上传图片
     * @param fileName String
     * @param file RequestBody
     * @return Call<Response>
     */
    @POST("/api/v1/upload_external_noform")
    fun upload(
        @Query("filename") fileName: String,
        @Body file: RequestBody
    ): Call<Response>

    companion object {
        private const val SERVER_API = "https://p.sda1.dev"
        /**
         * 创建retrofit实例
         * @return Sda1
         */
        fun createInstance(): Sda1 {
            return Retrofit.Builder().baseUrl(SERVER_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(Sda1::class.java)
        }

        /**
         * 上传图片
         * @param requestBody RequestBody
         * @param fileName String
         * @return Call<String>
         */
        fun uploadImage(requestBody: RequestBody, fileName: String): Call<String> {
            return ApiHelper.convertCall(createInstance().upload(fileName, requestBody)) {
                Log.v("rsp", it.toString())
                it.data?.url ?: ""
            }
        }
    }
}