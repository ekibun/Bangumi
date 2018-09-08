package soko.ekibun.bangumi.api.bgmlist

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import soko.ekibun.bangumi.api.bgmlist.bean.BgmItem
import soko.ekibun.bangumi.util.HttpUtil

interface Bgmlist{

    @GET("/tempapi/bangumi/{year}/{month}/json")
    fun query(@Path("year") year: Int,
               @Path("month") month: Int
    ): Call<Map<String, BgmItem>>

    companion object {
        private const val SERVER_API = "https://bgmlist.com"
        fun createInstance(): Bgmlist{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(HttpUtil.okHttpClient)
                    .build().create(Bgmlist::class.java)
        }
    }
}