package soko.ekibun.bangumi

import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun timeWrapper() {
        val time = "2019-06-22"

        val calendar = Calendar.getInstance()
        print("${calendar.time}\n")
        if (time.endsWith("ago")) {
            Regex("(\\d+)([dhm])").findAll(time).map { it.groupValues }.forEach {
                calendar.add(when (it[2]) {
                    "d" -> Calendar.DAY_OF_MONTH
                    "h" -> Calendar.HOUR
                    "m" -> Calendar.SECOND
                    else -> return@forEach
                }, -(it[1].toIntOrNull() ?: 0))
            }
            print(calendar.time)
        } else if (time.endsWith("前")) {
            Regex("(\\d+)(年|月|天|小时|分|秒)").findAll(time).map { it.groupValues }.forEach {
                calendar.add(when (it[2]) {
                    "年" -> Calendar.YEAR
                    "月" -> Calendar.MONTH
                    "天" -> Calendar.DAY_OF_MONTH
                    "小时" -> Calendar.HOUR
                    "分" -> Calendar.MINUTE
                    "秒" -> Calendar.SECOND
                    else -> return@forEach
                }, -(it[1].toIntOrNull() ?: 0))
            }
            print(calendar.time)
        }
        try {
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).parse(time)
        } catch (e: Exception) {
        }


    }
}
