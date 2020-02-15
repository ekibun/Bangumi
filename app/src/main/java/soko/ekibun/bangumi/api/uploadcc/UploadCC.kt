package soko.ekibun.bangumi.api.uploadcc

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.uploadcc.bean.Response

/**
 * 图床
 */
interface UploadCC {

    /**
     * 上传图片
     * @param fileToUpload Part
     * @return Call<Response>
     */
    @Multipart
    @POST("/image_upload")
    fun upload(@Part fileToUpload: MultipartBody.Part): Call<Response>

    companion object {
        private const val SERVER_API = "https://upload.cc"
        /**
         * 创建retrofit实例
         * @return UploadCC
         */
        fun createInstance(): UploadCC {
            return Retrofit.Builder().baseUrl(SERVER_API)
                .addConverterFactory(GsonConverterFactory.create())
                .build().create(UploadCC::class.java)
        }

        /**
         * 上传图片
         * @param requestBody RequestBody
         * @param fileName String
         * @return Call<String>
         */
        fun uploadImage(requestBody: RequestBody, fileName: String): Call<String> {
            val body = MultipartBody.Part.createFormData("uploaded_file[]", fileName, requestBody)
            return ApiHelper.convertCall(createInstance().upload(body)) {
                it.success_image?.firstOrNull()?.url?.let { "https://upload.cc/$it" } ?: ""
            }
        }
    }
}