package soko.ekibun.bangumi.api.catbox

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface CatBox {
    @Multipart
    @POST("/user/api.php")
    fun upload(@Part fileToUpload: MultipartBody.Part,
               @Part("reqtype") reqtype: String = "fileupload",
               @Part("userhash") userhash: String = ""
    ): Call<String>

    companion object {
        private const val SERVER_API = "https://catbox.moe"
        fun createInstance(): CatBox{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(ScalarsConverterFactory.create())
                    .build().create(CatBox::class.java)
        }
    }
}