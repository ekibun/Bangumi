package soko.ekibun.bangumi.api.uploadcc

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import soko.ekibun.bangumi.api.uploadcc.bean.Response

interface UploadCC {

    @Multipart
    @POST("/image_upload")
    fun upload(@Part fileToUpload: MultipartBody.Part): Call<Response>

    companion object {
        private const val SERVER_API = "https://upload.cc"
        fun createInstance(): UploadCC {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(UploadCC::class.java)
        }
    }
}