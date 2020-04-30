package soko.ekibun.bangumi.api.github

import io.reactivex.Observable
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.api.github.bean.Release
import soko.ekibun.bangumi.util.JsonUtil

/**
 * GitHub API
 */
object Github {

    private const val GITHUB_SERVER_API = "https://api.github.com"

    /**
     * 获取版本列表
     */
    fun releases(): Observable<List<Release>> {
        return ApiHelper.createHttpObservable(
            "$GITHUB_SERVER_API/repos/ekibun/Bangumi/releases"
        ).map {
            JsonUtil.toEntity<List<Release>>(it.body!!.string())
        }
    }

    private const val JSDELIVR_SERVER_API = "https://cdn.jsdelivr.net"

    /**
     * 时间表
     */
    fun bangumiCalendar(): Observable<List<BangumiCalendarItem>> {
        return ApiHelper.createHttpObservable(
            "$JSDELIVR_SERVER_API/gh/ekibun/bangumi_onair@master/calendar.json"
        ).map {
            JsonUtil.toEntity<List<BangumiCalendarItem>>(it.body!!.string())
        }
    }

    /**
     * 播放源
     * @param id Int
     */
    fun onAirInfo(id: Int): Observable<OnAirInfo> {
        return ApiHelper.createHttpObservable(
            "$JSDELIVR_SERVER_API/gh/ekibun/bangumi_onair@master/onair/${id / 1000}/$id.json"
        ).map {
            JsonUtil.toEntity<OnAirInfo>(it.body!!.string())
        }
    }
}