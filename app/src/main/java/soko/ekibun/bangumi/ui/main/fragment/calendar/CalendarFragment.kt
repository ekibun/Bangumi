package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.View
import com.oushangfeng.pinnedsectionitemdecoration.SmallPinnedHeaderItemDecoration
import kotlinx.android.synthetic.main.content_calendar.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Calendar
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.api.bgmlist.Bgmlist
import soko.ekibun.bangumi.api.bgmlist.bean.BgmItem
import soko.ekibun.bangumi.api.tinygrail.Tinygrail
import soko.ekibun.bangumi.api.tinygrail.bean.OnAir
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class CalendarFragment: DrawerFragment(R.layout.content_calendar) {
    override val titleRes: Int = R.string.calendar

    val api by lazy { Bangumi.createInstance() }
    private val calendarListAdapter = CalendarAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.savedInstanceState = savedInstanceState?:this.savedInstanceState

        calendar_list.adapter = calendarListAdapter
        calendar_list.layoutManager = LinearLayoutManager(context)
        calendar_list.addItemDecoration(
                SmallPinnedHeaderItemDecoration.Builder(R.id.item_date, CalendarAdapter.SECTION_HEADER).create()
        )
        calendarListAdapter.setOnItemChildClickListener { _, v, position ->
            calendarListAdapter.data[position].t?.let {
                SubjectActivity.startActivity(activity!!,  it.subject)
            }
        }

        calendar_swipe.setOnRefreshListener {
            loadCalendarList()
        }
        if(calendarListAdapter.data.size == 0){
            loadCalendarList()
        }
    }

    fun mixSubject(new:Subject, old:Subject?): Subject{
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

    var subjects: List<Subject>? = null
    var old: Map<Int, Map<String, List<OnAir>>>? = null
    private fun mixOnAir(new: Map<Int, Map<String,List<OnAir>>>): Map<Int, Map<String, List<OnAir>>>{
        val onAir = HashMap<Int, HashMap<String, ArrayList<OnAir>>>()
        if(calendarListAdapter.data.firstOrNull()?.t?.episode != null){
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
        val now = CalendarAdapter.getNowInt()
        calendarListAdapter.setNewData(null)
        var index = 0
        it.toList().sortedBy { it.first }.forEach {date->
            var isHeader = true
            if(date.first == now)
                index = calendarListAdapter.data.size
            date.second.toList().sortedBy { it.first }.forEach { time->
                var showTime = true
                time.second.forEach {
                    it.subject = subjects?.firstOrNull { subject-> subject.id == it.subject.id }?:it.subject
                    if(it.subject.images != null){
                        calendarListAdapter.addData(CalendarAdapter.CalendarSection(isHeader, it, date.first, time.first, showTime))
                        isHeader = false
                        showTime = false
                    } } }
        }
        (calendar_list?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
    }

    private var calendarCall : Call<List<Calendar>>? = null
    private var bgmlistCall : Call<Map<String, BgmItem>>? = null
    private var onAirCall : Call<Map<Int, Map<String,List<OnAir>>>>? = null
    @SuppressLint("UseSparseArrays")
    private fun loadCalendarList(){
        calendar_swipe?.isRefreshing = true
        val now = CalendarAdapter.getNowInt()
        bgmlistCall?.cancel()
        bgmlistCall = Bgmlist.createInstance().query(now/10000, (now/100%100-1)/3*3+1)
        bgmlistCall?.enqueue(ApiHelper.buildCallback(calendar_swipe?.context, {
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
        onAirCall?.enqueue(ApiHelper.buildCallback(calendar_swipe?.context, {
            val onAir = HashMap<Int, HashMap<String, ArrayList<OnAir>>>()
            it.forEach {date->date.value.forEach {time->time.value.forEach {
                onAir.getOrPut(date.key){ HashMap() }.getOrPut(time.key){ ArrayList() }.add(it)
            } } }
            setOnAirList(mixOnAir(onAir))
        }, { calendar_swipe?.isRefreshing = false }))

        calendarCall?.cancel()
        calendarCall = api.calendar()
        calendarCall?.enqueue(ApiHelper.buildCallback(calendar_swipe?.context, {
            val list = ArrayList<Subject>()
            it.forEach { it.items?.let{list.addAll(it)} }
            subjects = list
            calendarListAdapter.data.forEachIndexed { index, calendarSection ->
                calendarListAdapter.data.getOrNull(index)?.t?.subject = list.firstOrNull { it.id == calendarSection.t.subject.id }?:calendarSection.t.subject
            }
            calendarListAdapter.setNewData(calendarListAdapter.data)
        }, {}))

    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_search)?.isVisible = true
        //menu.findItem(R.id.action_type)?.isVisible = false
        super.onPrepareOptionsMenu(menu)
    }
}