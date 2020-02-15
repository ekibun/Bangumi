package soko.ekibun.bangumi.api.github.bean

import soko.ekibun.bangumi.api.bangumi.bean.Episode

/**
 * 时间表条目项
 * @property id Int?
 * @property name String?
 * @property name_cn String?
 * @property air_date String?
 * @property weekDayJP Int?
 * @property weekDayCN Int?
 * @property timeJP String?
 * @property timeCN String?
 * @property image String?
 * @property eps List<Episode>?
 * @constructor
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