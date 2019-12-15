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
interface Jsdelivr {

    /**
     * 时间表
     */
    @GET("/gh/ekibun/bangumi_onair/calendar.json")
    fun bangumiCalendar(): Call<List<BangumiCalendarItem>>

    /**
     * 播放源
     */
    @GET("/gh/ekibun/bangumi_onair/onair/{prefix}/{id}.json")
    fun onAirInfo(@Path("prefix") prefix: Int,
                  @Path("id") id: Int): Call<OnAirInfo>

    companion object {
        private const val SERVER_API = "https://cdn.jsdelivr.net"
        /**
         * 创建retrofit实例
         */
        fun createInstance(): Jsdelivr {
            return Retrofit.Builder().baseUrl(SERVER_API)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build().create(Jsdelivr::class.java)
        }
    }
}