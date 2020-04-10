package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import kotlinx.android.synthetic.main.item_calendar.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil
import java.text.DecimalFormat
import java.util.*

/**
 * 时间表Adapter
 * @constructor
 */
class CalendarAdapter(data: MutableList<CalendarSection>? = null) :
    BaseSectionQuickAdapter<CalendarAdapter.CalendarSection, BaseViewHolder>
        (R.layout.item_calendar, R.layout.item_calendar, data) {
    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: CalendarSection) {
        helper.addOnClickListener(R.id.item_layout)
        helper.setText(R.id.item_title, item.t.subject.displayName)
        helper.setText(
            R.id.item_ep_name,
            item.t.episode?.parseSort() + " " + (if (item.t.episode?.name_cn.isNullOrEmpty()) item.t.episode?.name
                ?: "" else item.t.episode?.name_cn)
        )
        GlideUtil.with(helper.itemView.item_cover)
            ?.load(Images.small(item.t.subject.image))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
            ?.into(helper.itemView.item_cover)
        helper.itemView.item_time.text = ""
        helper.itemView.item_chase.visibility = if (item.t.subject.collect != null) View.VISIBLE else View.GONE

        val use30h = App.app.sp.getBoolean("calendar_use_30h", false)

        val past = pastTime(item.date, item.time, use30h)
        val color = ResourceUtil.resolveColorAttr(
            helper.itemView.context,
            if (past) R.attr.colorPrimary else android.R.attr.textColorSecondary
        )
        helper.itemView.item_ep_name.setTextColor(color)
        helper.itemView.item_time.alpha = if (past) 0.6f else 1.0f

        helper.itemView.item_now_time.visibility = View.GONE

        if (item.date != getNowInt(use30h)) return
        val index = data.indexOfFirst { it === item }
        if ((index + 1 == data.size && past) || ((data.getOrNull(index - 1)?.let {
                pastTime(
                    it.date,
                    it.time,
                    use30h
                )
            } != false) != past)) {
            if (index + 1 == data.size && past) {//最后一个
                helper.itemView.item_now_time.bringToFront()
            } else {
                helper.itemView.item_layout.bringToFront()
            }
            helper.itemView.item_now_time.visibility = View.VISIBLE

            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            val hourNow = if (use30h) (hour - 6 + 24) % 24 + 6 else hour
            val minuteNow = cal.get(Calendar.MINUTE)
            val format = DecimalFormat("00")
            helper.itemView.item_now_time_text.text = "${format.format(hourNow)}:${format.format(minuteNow)}"
        }
    }

    override fun convertHead(helper: BaseViewHolder, item: CalendarSection) {
        convert(helper, item)
        helper.setText(R.id.item_time, item.time)
    }

    /**
     * 时间表项（带section）
     * @property date Int
     * @property time String
     * @constructor
     */
    class CalendarSection(isHeader: Boolean, subject: OnAir, var date: Int, var time: String) :
        SectionEntity<OnAir>(isHeader, "") {
        init {
            t = subject
        }
    }

    /**
     * 放送项数据
     * @property episode Episode?
     * @property subject Subject
     * @constructor
     */
    data class OnAir(
        var episode: Episode?,
        var subject: Subject
    )

    companion object {
        /**
         * 转换时间
         * @param date Int
         * @param time String
         * @param use_30h Boolean
         * @return Boolean
         */
        fun pastTime(date: Int, time: String, use_30h: Boolean): Boolean {
            val nowInt = getNowInt(use_30h)
            if (nowInt > date) return true
            else if (nowInt < date) return false
            val match = Regex("""([0-9]*):([0-9]*)""").find(time)
            val hour = match?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val minute = match?.groupValues?.get(2)?.toIntOrNull() ?: 0
            val cal = Calendar.getInstance()
            val hourNow = cal.get(Calendar.HOUR_OF_DAY)
            val hourNow30h = if (use_30h) (hourNow - 6 + 24) % 24 + 6 else hourNow
            val minuteNow = cal.get(Calendar.MINUTE)
            return hour < hourNow30h || (hour == hourNow30h && minute < minuteNow)
        }

        /**
         * Int -> Calendar
         * @param date Int
         * @return Calendar
         */
        fun getIntCalendar(date: Int): Calendar {
            val cal = Calendar.getInstance()
            cal.set(date / 10000, date / 100 % 100 - 1, date % 100)
            return cal
        }

        /**
         * Calendar -> Int
         * @param now Calendar
         * @return Int
         */
        fun getCalendarInt(now: Calendar): Int {
            return now.get(Calendar.YEAR) * 10000 + (now.get(Calendar.MONTH) + 1) * 100 + now.get(Calendar.DATE)
        }

        /**
         * 获取星期
         * @param now Calendar
         * @return Int
         */
        fun getWeek(now: Calendar): Int {
            val isFirstSunday = now.firstDayOfWeek == Calendar.SUNDAY
            var weekDay = now.get(Calendar.DAY_OF_WEEK)
            if (isFirstSunday) {
                weekDay -= 1
                if (weekDay == 0) {
                    weekDay = 7
                }
            }
            return weekDay
        }

        /**
         * 获取当前时间（Int）
         * @param use_30h Boolean
         * @return Int
         */
        fun getNowInt(use_30h: Boolean): Int {
            val cal = Calendar.getInstance()
            val hourNow = cal.get(Calendar.HOUR_OF_DAY)
            cal.add(Calendar.DAY_OF_MONTH, if (hourNow < if (use_30h) 6 else 0) -1 else 0)
            return getCalendarInt(cal)
        }
    }
}