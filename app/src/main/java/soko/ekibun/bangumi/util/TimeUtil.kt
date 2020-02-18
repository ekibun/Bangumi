package soko.ekibun.bangumi.util

import android.annotation.SuppressLint
import java.text.SimpleDateFormat

/**
 * 时间格式化
 */
@SuppressLint("SimpleDateFormat")
object TimeUtil {
    val dateFormat by lazy { SimpleDateFormat("yyyy-MM-dd") }
    val timeFormat by lazy { SimpleDateFormat("HH:mm") }

    val weekJp = listOf("", "月", "火", "水", "木", "金", "土", "日")
    val weekList = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
    val weekSmall = listOf("", "一", "二", "三", "四", "五", "六", "日")
}