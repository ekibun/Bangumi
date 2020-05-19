package soko.ekibun.bangumi.api.github

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.api.github.bean.OnAirInfo
import soko.ekibun.bangumi.api.github.bean.Release
import soko.ekibun.bangumi.util.HttpUtil
import soko.ekibun.bangumi.util.JsonUtil

/**
 * GitHub API
 */
object Github {

    private const val GITHUB_SERVER_API = "https://api.github.com"

    /**
     * 获取版本列表
     */
    suspend fun releases(): List<Release> {
        return withContext(Dispatchers.IO) {
            JsonUtil.toEntity<List<Release>>(
                HttpUtil.getCall(
                    "$GITHUB_SERVER_API/repos/ekibun/Bangumi/releases"
                ).execute().body!!.string()
            )!!
        }
    }

    private const val JSDELIVR_SERVER_API = "https://cdn.jsdelivr.net"

    /**
     * 时间表
     */
    suspend fun bangumiCalendar(): List<BangumiCalendarItem> {
        return withContext(Dispatchers.IO) {
            JsonUtil.toEntity<List<BangumiCalendarItem>>(
                HttpUtil.getCall(
                    "$JSDELIVR_SERVER_API/gh/ekibun/bangumi_onair@master/calendar.json"
                ).execute().body!!.string()
            )!!
        }
    }

    /**
     * 播放源
     * @param id Int
     */
    suspend fun onAirInfo(id: Int): OnAirInfo? {
        return withContext(Dispatchers.IO) {
            JsonUtil.toEntity<OnAirInfo>(
                HttpUtil.getCall(
                    "$JSDELIVR_SERVER_API/gh/ekibun/bangumi_onair@master/onair/${id / 1000}/$id.json"
                ).execute().body?.string() ?: ""
            )
        }
    }
}