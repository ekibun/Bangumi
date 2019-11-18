package soko.ekibun.bangumi.api.github

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.api.github.bean.OnAirInfo

/**
 * Github repo数据
 */
interface GithubRaw{

    /**
     * 时间表
     */
    @GET("/ekibun/bangumi_onair/master/calendar.json")
    fun bangumiCalendar(): Call<List<BangumiCalendarItem>>

    /**
     * 播放源
     */
    @GET("/ekibun/bangumi_onair/master/onair/{prefix}/{id}.json")
    fun onAirInfo(@Path("prefix") prefix: Int,
                  @Path("id") id: Int): Call<OnAirInfo>

    companion object {
        private const val SERVER_API = "https://raw.githubusercontent.com"
        /**
         * 创建retrofit实例
         */
        fun createInstance(): GithubRaw{
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(GithubRaw::class.java)
        }
    }
}