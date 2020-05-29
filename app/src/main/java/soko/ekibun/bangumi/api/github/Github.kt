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
                HttpUtil.fetch(
                    "$GITHUB_SERVER_API/repos/ekibun/Bangumi/releases"
                ).body!!.string()
            )!!
        }
    }

    private const val JSDELIVR_SERVER_API = "https://cdn.jsdelivr.net"

    /**
     * 时间表
     */
    suspend fun bangumiCalendar(): List<BangumiCalendarItem> {
        return withContext(Dispatchers.IO) {
            if (latestTag.isEmpty()) updateOnAirLatestTag()
            JsonUtil.toEntity<List<BangumiCalendarItem>>(
                HttpUtil.fetch(
                    "$JSDELIVR_SERVER_API/gh/ekibun/bangumi_onair$latestTag/calendar.json"
                ).body!!.string()
            )!!
        }
    }

    private var latestTag = ""
        get() = if (field.isEmpty()) "" else "@$field"
    private var lastUpdate = 0L
    private suspend fun updateOnAirLatestTag() {
        withContext(Dispatchers.IO) {
            try {
                val curTime = System.currentTimeMillis()
                if (curTime - lastUpdate < 60 * 60 * 1000L) return@withContext
                lastUpdate = curTime
                latestTag = HttpUtil.fetch(
                    "https://github.com/ekibun/bangumi_onair/releases/latest", HttpUtil.RequestOption(
                        followRedirect = false
                    )
                ).headers["Location"]?.substringAfterLast('/') ?: latestTag
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 播放源
     * @param id Int
     */
    suspend fun onAirInfo(id: Int): OnAirInfo? {
        return withContext(Dispatchers.IO) {
            updateOnAirLatestTag()
            JsonUtil.toEntity<OnAirInfo>(
                HttpUtil.fetch(
                    "$JSDELIVR_SERVER_API/gh/ekibun/bangumi_onair$latestTag/onair/${id / 1000}/$id.json"
                ).body?.string() ?: ""
            )
        }
    }
}