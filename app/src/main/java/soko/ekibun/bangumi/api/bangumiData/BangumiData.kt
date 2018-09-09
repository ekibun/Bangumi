package soko.ekibun.bangumi.api.bangumiData

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import soko.ekibun.bangumi.api.bangumiData.bean.BangumiItem
import soko.ekibun.bangumi.util.HttpUtil

interface BangumiData{

    @GET("/bangumi-data/bangumi-data/master/data/items/{year}/{month}.json")
    fun query(@Path("year") year: Int,
              @Path("month") month: String
    ): Call<List<BangumiItem>>

    companion object {
        private const val SERVER_API = "https://raw.githubusercontent.com"
        fun createInstance(): BangumiData{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(BangumiData::class.java)
        }
    }
}