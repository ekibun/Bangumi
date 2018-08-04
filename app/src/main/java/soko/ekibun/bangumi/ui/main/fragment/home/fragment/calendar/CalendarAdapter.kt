package soko.ekibun.bangumi.ui.main.fragment.home.fragment.calendar

import android.support.v7.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseSectionQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.chad.library.adapter.base.entity.SectionEntity
import com.oushangfeng.pinnedsectionitemdecoration.utils.FullSpanUtil
import kotlinx.android.synthetic.main.header_calendar.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.ResourceUtil
import java.util.*

class CalendarAdapter(data: MutableList<CalendarSection>? = null) :
        BaseSectionQuickAdapter<CalendarAdapter.CalendarSection, BaseViewHolder>
        (R.layout.item_calendar, R.layout.header_calendar, data) {
    override fun convert(helper: BaseViewHolder, item: CalendarSection) {
        helper.setText(R.id.item_title, if(item.t.name_cn.isNullOrEmpty()) item.t.name else item.t.name_cn)
        helper.setText(R.id.item_name_jp, item.t.name)
        helper.addOnClickListener(R.id.item_layout)
        Glide.with(helper.itemView)
                .load(item.t.images?.common)
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(helper.itemView.item_cover)
    }

    override fun convertHead(helper: BaseViewHolder, item: CalendarSection) {
        helper.setText(R.id.item_date, weekSmall[item.week])
        helper.setText(R.id.item_date_jp, weekJp[item.week])

        val color = ResourceUtil.resolveColorAttr(helper.itemView.context,
                if(currentWeek() == item.week) R.attr.colorPrimary else android.R.attr.textColorSecondary)
        helper.itemView.item_date.setTextColor(color)
        helper.itemView.item_date_jp.setTextColor(color)

        convert(helper, item)
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        FullSpanUtil.onAttachedToRecyclerView(recyclerView, this, SECTION_HEADER_VIEW)
    }

    override fun onViewAttachedToWindow(holder: BaseViewHolder) {
        super.onViewAttachedToWindow(holder)
        FullSpanUtil.onViewAttachedToWindow(holder, this, SECTION_HEADER_VIEW)
    }

    class CalendarSection(isHeader: Boolean, subject: Subject, var week: Int) : SectionEntity<Subject>(isHeader, ""){
        init{
            t = subject
        }
    }

    companion object {
        val weekJp = listOf("", "月", "火", "水", "木", "金", "土", "日")
        val weekSmall = listOf("", "周一", "周二", "周三", "周四", "周五", "周六", "周日")
        const val SECTION_HEADER = SECTION_HEADER_VIEW

        fun currentWeek():Int{
            val now = Calendar.getInstance()
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
    }
}