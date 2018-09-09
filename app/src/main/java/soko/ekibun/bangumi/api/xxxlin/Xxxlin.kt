package soko.ekibun.bangumi.api.xxxlin

import android.os.Build
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.POST
import soko.ekibun.bangumi.api.xxxlin.bean.BaseResult
import soko.ekibun.bangumi.util.HttpUtil
import java.util.*

interface Xxxlin {
    @FormUrlEncoded
    @POST("/api/soko/bangumi/v1/log/add")
    fun crashReport(@Field("content") content: String,
                    @Field("appVersionCode") appVersionCode: Int,
                    @Field("appVersionName") appVersionName: String,
                    @Field("deviceManufacturer") deviceManufacturer: String = Build.MANUFACTURER,
                    @Field("deviceBrand") deviceBrand: String = Build.BRAND,
                    @Field("deviceModel") deviceModel: String = Build.MODEL,
                    @Field("deviceLanguage") deviceLanguage: String = Locale.getDefault().toString(),
                    @Field("deviceVersionRelease") deviceVersionRelease: String = Build.VERSION.RELEASE,
                    @Field("deviceSdkInt") deviceSdkInt: Int = Build.VERSION.SDK_INT): Call<BaseResult>

    companion object {
        private const val SERVER_API = "http://www.Xxxlin.com"
        fun createInstance(): Xxxlin{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(Xxxlin::class.java)
        }
    }
}