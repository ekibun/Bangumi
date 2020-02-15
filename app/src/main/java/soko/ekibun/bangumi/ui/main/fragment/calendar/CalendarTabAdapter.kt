package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import com.nshmura.recyclertablayout.RecyclerTabLayout
import kotlinx.android.synthetic.main.item_calendar_tab.view.*
import soko.ekibun.bangumi.R

/**
 * 时间表TabAdapter
 * @property viewpager ViewPager
 * @constructor
 */
class CalendarTabAdapter(val viewpager: ViewPager) : RecyclerTabLayout.Adapter<CalendarTabAdapter.ViewHolder>(viewpager) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_calendar_tab, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return viewPager.adapter?.count?:0
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val posDate = (viewpager.adapter as? CalendarPagerAdapter)?.getPostDate(position)?:return
        val date = CalendarAdapter.getCalendarInt(posDate)
        holder.itemView.item_today.visibility = if(position == 7) View.VISIBLE else View.INVISIBLE
        holder.itemView.item_date.text = "${date/100%100}-${date%100}"
        holder.itemView.item_week.text = CalendarAdapter.weekSmall[CalendarAdapter.getWeek(posDate)]
        holder.itemView.item_week.isSelected = currentIndicatorPosition == position
        holder.itemView.item_date.isSelected = holder.itemView.item_week.isSelected
        holder.itemView.item_today.isSelected = holder.itemView.item_week.isSelected
        holder.itemView.item_week.isEnabled = position != 7
    }

    /**
     * 时间表Tab项
     * @constructor
     */
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            itemView.setOnClickListener {
                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    viewpager.setCurrentItem(pos, true)
                }
            }
        }
    }
}