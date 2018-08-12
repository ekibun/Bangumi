package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.support.v7.widget.RecyclerView
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import com.oushangfeng.pinnedsectionitemdecoration.utils.FullSpanUtil
import kotlinx.android.synthetic.main.item_calendar.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.tinygrail.bean.OnAir
import soko.ekibun.bangumi.util.ResourceUtil
import java.lang.StringBuilder
import java.util.*

class CalendarAdapter(data: MutableList<CalendarSection>? = null) :
        BaseSectionQuickAdapter<CalendarAdapter.CalendarSection, BaseViewHolder>
        (R.layout.item_calendar, R.layout.item_calendar, data) {
    override fun convert(helper: BaseViewHolder, item: CalendarSection) {
        helper.setText(R.id.item_title, if(item.t.subject.name_cn.isNullOrEmpty()) item.t.subject.name else item.t.subject.name_cn)
        helper.setText(R.id.item_name_jp, if(item.t.episode?.name_cn.isNullOrEmpty()) item.t.episode?.name?:"" else item.t.episode?.name_cn)
        helper.addOnClickListener(R.id.item_layout)
        Glide.with(helper.itemView)
                .load(item.t.subject.images?.common)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(helper.itemView.item_cover)
        helper.itemView.item_time.text = if(item.showTime) item.time else ""
        helper.itemView.item_date_2.visibility = View.GONE
        helper.itemView.item_date_1.visibility = View.INVISIBLE
    }

    override fun convertHead(helper: BaseViewHolder, item: CalendarSection) {
        convert(helper, item)

        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                if(getNowInt() == item.date) R.attr.colorPrimary else android.R.attr.textColorSecondary)
        helper.itemView.item_date_1.setTextColor(color)
        helper.itemView.item_date_2.setTextColor(color)

        helper.itemView.item_date_1.visibility = View.VISIBLE
        helper.itemView.item_date_2.visibility = View.VISIBLE
        helper.setText(R.id.item_time, item.time)
        helper.setText(R.id.item_date_1, parseDate1(item.date))
        helper.setText(R.id.item_date_2, parseDate2(item.date))
    }

    private fun parseDate1(date: Int): String{
        return "${date/100%100}-${date%100}"
    }
    private fun parseDate2(date: Int): String{
        val cal = getIntCalendar(date)
        return "${weekSmall[getWeek(cal)]}(${weekJp[getWeek(cal)]})"
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, SECTION_HEADER_VIEW)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, SECTION_HEADER_VIEW)
    }

    class CalendarSection(isHeader: Boolean, subject: OnAir, var date: Int, var time: String, var showTime: Boolean) : SectionEntity<OnAir>(isHeader, ""){
        init{
            t = subject
        }
    }

    companion object {
        val weekJp = listOf("", "月", "火", "水", "木", "金", "土", "日")
        val weekSmall = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        const val SECTION_HEADER = SECTION_HEADER_VIEW

        fun getIntCalendar(date: Int):Calendar{
            val cal = Calendar.getInstance()
            cal.set(date/10000, date/100%100-1, date%100)
            return cal
        }

        fun getCalendarInt(now: Calendar):Int{
            return now.get(Calendar.YEAR)*10000 + (now.get(Calendar.MONTH)+1) * 100 + now.get(Calendar.DATE)
        }

        fun getWeek(now: Calendar): Int{
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

        fun getNowInt():Int{
            return getCalendarInt(Calendar.getInstance())
        }

        fun currentWeek():Int{
            val now = Calendar.getInstance()
            return getWeek(now)
        }
    }
}