package soko.ekibun.bangumi.api.github.bean

import soko.ekibun.bangumi.api.bangumi.bean.Episode

/**
 * 时间表条目项
 */
data class BangumiCalendarItem(
        val id: Int?,
        val name: String?,
        val name_cn: String?,
        val air_date: String?,
        val weekDayJP: Int?,
        val weekDayCN: Int?,
        val timeJP: String?,
        val timeCN: String?,
        val image: String?,
        val eps: List<Episode>?
) {
}