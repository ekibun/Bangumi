package soko.ekibun.bangumi.api.github.bean

import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.ui.main.fragment.calendar.CalendarAdapter
import soko.ekibun.bangumi.util.TimeUtil
import java.util.*

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
        val eps: List<Episode>?
) {
    fun getEpisodeDateTime(ep: Episode): Pair<Int, String> {
        val useCN = App.app.sp.getBoolean("calendar_use_cn", false)
        val use30h = App.app.sp.getBoolean("calendar_use_30h", false)

        val useCNTime = useCN && !timeCN.isNullOrEmpty() // 判断是否使用国内时间
        val timeInt = (if (useCNTime) timeCN else timeJP)?.toIntOrNull() ?: -1
        val weekInt = (if (useCNTime) weekDayCN else weekDayJP) ?: 0

        // 根据airdate创建日期对象
        val cal = Calendar.getInstance()
        cal.time = try {
            TimeUtil.dateFormat.parse(ep.airdate ?: "")
        } catch (e: Exception) {
            null
        } ?: cal.time

        if (timeInt < 0) {
          return CalendarAdapter.getCalendarInt(cal) to "??:??"
        }
        val zoneOffset = TimeZone.getDefault().rawOffset / 1000 / 60    // 时差（min）
        val hourDif = zoneOffset / 60 - 8           // 小时差（源数据是UTC+8，减8）
        val minuteDif = zoneOffset % 60             // 分钟差
        val minute = timeInt % 100 + minuteDif      // 分钟 + 分钟差
        val hour = timeInt / 100 + hourDif + when { // 小时 + 小时差 + 分钟的进位
            minute >= 60 -> 1   // 大于60，进1位
            minute < 0 -> -1    // 小于0，退1位
            else -> 0
        }
        val dayCarry = when {               // 日期进位
            hour >= if (use30h) 30 else 24 -> 1  // 30小时制大于30进位，否则大于24进位
            hour < if (use30h) 6 else 0 -> -1    // 30小时制小于6退位，否则小于0退位
            else -> 0
        }
        val dayDif = dayCarry + if (
            timeInt / 100 < (if (useCNTime) 5 else 6) && // 假设airdate按30小时算，且国内放送时间与日本相同，若日本放送时间<6:00，取次日
            (weekInt == 0 || (CalendarAdapter.getWeek(cal) - weekInt + 7) % 7 > 0) // 若日本放送星期与airdate相同，取0
        ) 1 else 0
        cal.add(Calendar.DAY_OF_MONTH, dayDif)   // 加上日期差，计算日期
        // 格式化日期 -> hh:mm
        val time = String.format(
            "%02d:%02d",
            if (use30h) (hour - 6 + 24) % 24 + 6 else (hour + 24) % 24, minute % 60
        )
        val date = CalendarAdapter.getCalendarInt(cal)
        return date to time
    }
}