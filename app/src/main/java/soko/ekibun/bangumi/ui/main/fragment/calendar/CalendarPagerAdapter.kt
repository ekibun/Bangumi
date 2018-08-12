package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.annotation.SuppressLint
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Calendar
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.api.bgmlist.Bgmlist
import soko.ekibun.bangumi.api.bgmlist.bean.BgmItem
import soko.ekibun.bangumi.api.tinygrail.Tinygrail
import soko.ekibun.bangumi.api.tinygrail.bean.OnAir
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class CalendarPagerAdapter(val fragment: CalendarFragment, private val pager: ViewPager) : PagerAdapter(){

    private fun getItem(position: Int): Pair<CalendarAdapter, RecyclerView>{
        return items.getOrPut(position){
            val recyclerView = RecyclerView(pager.context)
            val adapter = CalendarAdapter()
            adapter.setOnItemChildClickListener { _, v, pos ->
                SubjectActivity.startActivity(v.context, adapter.data[pos].t.subject)
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(pager.context)
            recyclerView.isNestedScrollingEnabled = false
            Pair(adapter, recyclerView)
        }
    }

    private val items = LinkedHashMap<Int, Pair<CalendarAdapter, RecyclerView>>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = getItem(position)
        if(old == null)
            loadCalendarList()
        container.addView(item.second)
        return item.second
    }


    private var subjects: List<Subject>? = null
    private var old: Map<Int, Map<String, List<OnAir>>>? = null
    private fun mixOnAir(new: Map<Int, Map<String,List<OnAir>>>): Map<Int, Map<String, List<OnAir>>>{
        val onAir = LinkedHashMap<Int, HashMap<String, ArrayList<OnAir>>>()
        if(old?.values?.firstOrNull()?.values?.firstOrNull()?.firstOrNull()?.episode != null){
            old?.forEach {date->date.value.forEach {time->time.value.forEach {
                it.subject = mixSubject(it.subject, findSubject(new, it.subject))
                onAir.getOrPut(date.key){ HashMap() }.getOrPut(time.key){ ArrayList() }.add(it)
            } } }
            new.forEach {date->date.value.forEach {time->time.value.filter { findSubject(old, it.subject) == null }.forEach {
                onAir.getOrPut(date.key){ HashMap() }.getOrPut(time.key){ ArrayList() }.add(it)
            } } }
        }else{
            new.forEach {date->date.value.forEach {time->time.value.forEach {
                it.subject = mixSubject(it.subject, findSubject(old, it.subject))
                onAir.getOrPut(date.key){ HashMap() }.getOrPut(time.key){ ArrayList() }.add(it)
            } } }
            old?.forEach {date->date.value.forEach {time->time.value.filter { findSubject(new, it.subject) == null }.forEach {
                onAir.getOrPut(date.key){ HashMap() }.getOrPut(time.key){ ArrayList() }.add(it)
            } } }
        }
        old = onAir
        return onAir
    }

    private fun setOnAirList(it: Map<Int, Map<String,List<OnAir>>>){
        it.toList().sortedBy { it.first }.forEachIndexed {i, date->
            var index = -1
            val item = getItem(i)
            item.first.setNewData(null)
            date.second.toList().sortedBy { it.first }.forEach { time->
                var isHeader = true
                time.second.forEach {
                    it.subject = subjects?.firstOrNull { subject-> subject.id == it.subject.id }?:it.subject
                    if(it.subject.images != null){
                        if(index == -1 && !CalendarAdapter.pastTime(date.first, time.first))
                            index = item.first.data.size
                        item.first.addData(CalendarAdapter.CalendarSection(isHeader, it, date.first, time.first))
                        isHeader = false
                    } } }
            (item.second.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(index-1, 0)
        }
        notifyDataSetChanged()
        pager.currentItem = 7
    }

    private var calendarCall : Call<List<Calendar>>? = null
    private var bgmlistCall : Call<Map<String, BgmItem>>? = null
    private var onAirCall : Call<Map<Int, Map<String, List<OnAir>>>>? = null
    @SuppressLint("UseSparseArrays")
    fun loadCalendarList(){
        val now = CalendarAdapter.getNowInt()
        bgmlistCall?.cancel()
        bgmlistCall = Bgmlist.createInstance().query(now/10000, (now/100%100-1)/3*3+1)
        bgmlistCall?.enqueue(ApiHelper.buildCallback(pager.context, {
            val onAir = HashMap<Int, HashMap<String, ArrayList<OnAir>>>()
            val weekNow = CalendarAdapter.currentWeek() % 7
            it.values.forEach {
                val item=OnAir(null, Subject(it.bgmId, "${Bangumi.SERVER}/subject/${it.bgmId}", SubjectType.ANIME, it.titleJP, it.titleCN), null)
                val week = if(it.timeCN.isNullOrEmpty()) it.weekDayJP else it.weekDayCN
                val timeInt = (if(it.timeCN.isNullOrEmpty()) it.timeJP else it.timeCN)?.toIntOrNull()?:0
                val mStrBuilder = StringBuilder()
                val mFormatter = Formatter(mStrBuilder, Locale.getDefault())
                mStrBuilder.setLength(0)
                val time = mFormatter.format("%02d:%02d", timeInt/100, timeInt%100).toString()
                val cal = CalendarAdapter.getIntCalendar(now)
                cal.add(java.util.Calendar.DAY_OF_MONTH, -((weekNow - week)+7)%7)
                onAir.getOrPut(CalendarAdapter.getCalendarInt(cal)){HashMap()}.getOrPut(time){ArrayList()}.add(item)
                cal.add(java.util.Calendar.DAY_OF_MONTH, +7)
                onAir.getOrPut(CalendarAdapter.getCalendarInt(cal)){HashMap()}.getOrPut(time){ArrayList()}.add(item)
                cal.add(java.util.Calendar.DAY_OF_MONTH, -14)
                if(week == weekNow)
                    onAir.getOrPut(CalendarAdapter.getCalendarInt(cal)){HashMap()}.getOrPut(time){ArrayList()}.add(item)
            }
            setOnAirList(mixOnAir(onAir))
        }, {}))

        onAirCall?.cancel()
        onAirCall = Tinygrail.onAirList()
        onAirCall?.enqueue(ApiHelper.buildCallback(pager.context, {
            val onAir = HashMap<Int, HashMap<String, ArrayList<OnAir>>>()
            it.forEach {date->date.value.forEach {time->time.value.forEach {
                onAir.getOrPut(date.key){ HashMap() }.getOrPut(time.key){ ArrayList() }.add(it)
            } } }
            setOnAirList(mixOnAir(onAir))
        }, {}))

        calendarCall?.cancel()
        calendarCall = Bangumi.createInstance().calendar()
        calendarCall?.enqueue(ApiHelper.buildCallback(pager.context, {
            val list = ArrayList<Subject>()
            it.forEach { it.items?.let{list.addAll(it)} }
            subjects = list
            setOnAirList(mixOnAir(old?:HashMap()))
        }, {}))

    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    private fun parseDate(date: Int): String{
        val cal = CalendarAdapter.getIntCalendar(date)
        return "${date/100%100}-${date%100}\n${CalendarAdapter.weekSmall[CalendarAdapter.getWeek(cal)]}(${CalendarAdapter.weekJp[CalendarAdapter.getWeek(cal)]})"
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return parseDate(old?.toList()?.sortedBy { it.first }?.get(pos)?.first?:0)
    }

    override fun getCount(): Int {
        return old?.size?:0
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    companion object {
        private fun mixSubject(new:Subject, old:Subject?): Subject{
            return Subject(new.id,
                    "${Bangumi.SERVER}/subject/${new.id}",
                    SubjectType.ANIME,
                    if(new.name.isNullOrEmpty()) old?.name else new.name,
                    if(new.name_cn.isNullOrEmpty()) old?.name_cn else new.name_cn,
                    images = new.images?:old?.images)
        }

        private fun findSubject(map: Map<Int, Map<String,List<OnAir>>>?, subject: Subject):Subject?{
            map?.forEach {it.value.forEach { it.value.forEach {
                if(it.subject.id == subject.id)
                    return it.subject
            }}}
            return null
        }
    }
}