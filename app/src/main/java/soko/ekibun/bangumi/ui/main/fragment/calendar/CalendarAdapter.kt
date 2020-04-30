package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.entity.SectionEntity
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_calendar.view.*
import kotlinx.android.synthetic.main.item_calendar_now.view.*
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
        (R.layout.item_calendar_now, R.layout.item_calendar, data) {
    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: CalendarSection) {
        holder.setText(
            R.id.item_time,
            if (getItemOrNull(holder.adapterPosition - 1)?.time == item.time) "" else item.time
        )
        holder.setText(R.id.item_title, item.t.subject.displayName)
        holder.setText(
            R.id.item_ep_name,
            (if (item.t.episode?.id != 0) item.t.episode?.parseSort() + " " else "")
                    + (if (item.t.episode?.name_cn.isNullOrEmpty()) item.t.episode?.name
                ?: "" else item.t.episode?.name_cn)
        )
        GlideUtil.with(holder.itemView.item_cover)
            ?.load(Images.small(item.t.subject.image))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
            ?.into(holder.itemView.item_cover)
        holder.itemView.item_chase.visibility = if (item.t.subject.collect != null) View.VISIBLE else View.GONE

        holder.itemView.item_title.setTextColor(
            ResourceUtil.resolveColorAttr(
                holder.itemView.context,
                if (item.t.episode?.id != 0) android.R.attr.textColorPrimary else android.R.attr.textColorSecondary
            )
        )
        holder.itemView.item_cover.alpha = if (item.t.episode?.id != 0) 1.0f else 0.6f

        val color = ResourceUtil.resolveColorAttr(
            holder.itemView.context,
            if (item.t.episode?.id != 0 && item.past) R.attr.colorPrimary else android.R.attr.textColorSecondary
        )
        holder.itemView.item_ep_name.setTextColor(color)
        holder.itemView.item_time.alpha = if (item.past) 0.6f else 1.0f
    }

    override fun convertHeader(helper: BaseViewHolder, item: CalendarSection) {
        val use30h = App.app.sp.getBoolean("calendar_use_30h", false)
        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val hourNow = if (use30h) (hour - 6 + 24) % 24 + 6 else hour
        val minuteNow = cal.get(Calendar.MINUTE)
        val format = DecimalFormat("00")
        helper.itemView.item_now_time_text.text = "${format.format(hourNow)}:${format.format(minuteNow)}"
    }

    /**
     * 时间表项（带section）
     * @property date Int
     * @property time String
     * @constructor
     */
    class CalendarSection(override val isHeader: Boolean) : SectionEntity {
        var date: Int = 0
        var time: String = ""
        var past: Boolean = false
        lateinit var t: OnAir

        constructor(subject: OnAir, date: Int, time: String) : this(false) {
            this.t = subject
            this.date = date
            this.time = time
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