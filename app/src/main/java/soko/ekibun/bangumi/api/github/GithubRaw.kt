package soko.ekibun.bangumi.api.github

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.api.github.bean.BangumiItem
import soko.ekibun.bangumi.api.github.bean.OnAirInfo

interface GithubRaw{

    @GET("/bangumi-data/bangumi-data/master/data/items/{year}/{month}.json")
    fun bangumiData(@Path("year") year: Int,
                    @Path("month") month: String
    ): Call<List<BangumiItem>>

    @GET("/ekibun/bangumi_calendar/master/calendar.json")
    fun bangumiCalendar(): Call<List<BangumiCalendarItem>>

    @GET("/ekibun/bangumi_onair/master/onair/{prefix}/{id}.json")
    fun onAirInfo(@Path("prefix") prefix: Int,
                  @Path("id") id: Int): Call<OnAirInfo>

    companion object {
        private const val SERVER_API = "https://raw.githubusercontent.com"
        fun createInstance(): GithubRaw{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(GithubRaw::class.java)
        }
    }
}