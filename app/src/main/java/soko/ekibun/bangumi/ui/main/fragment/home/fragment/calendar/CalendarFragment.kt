package soko.ekibun.bangumi.ui.main.fragment.home.fragment.calendar

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.Menu
import android.view.View
import com.google.gson.reflect.TypeToken
import com.oushangfeng.pinnedsectionitemdecoration.SmallPinnedHeaderItemDecoration
import kotlinx.android.synthetic.main.content_calendar.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Calendar
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.HomeTabFragment
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.util.JsonUtil

class CalendarFragment: HomeTabFragment(R.layout.content_calendar) {
    override val titleRes: Int = R.string.calendar
    override val iconRes: Int = R.drawable.ic_calendar

    val api by lazy { Bangumi.createInstance() }
    private val calendarListAdapter = CalendarAdapter()

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val json = this.savedInstanceState?.getString("CalendarData","")?:""
        outState.putString("CalendarData", if(calendarListAdapter.data.size== 0) json else JsonUtil.toJson(calendarListAdapter.data))
        calendar_list?.let {
            it.layoutManager?.onSaveInstanceState()?.let {
                outState.putParcelable("CalendarList", it)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        this.savedInstanceState = savedInstanceState?:this.savedInstanceState

        calendar_list.adapter = calendarListAdapter
        calendar_list.layoutManager = LinearLayoutManager(context)
        calendar_list.addItemDecoration(
                SmallPinnedHeaderItemDecoration.Builder(R.id.item_pinned_header, CalendarAdapter.SECTION_HEADER).create()
        )
        calendarListAdapter.setOnItemChildClickListener { _, v, position ->
            calendarListAdapter.data[position].t?.let {
                SubjectActivity.startActivity(v.context, it)
            }
        }

        calendar_swipe.setOnRefreshListener {
            loadCalendarList()
        }
        if(this.savedInstanceState != null){
            calendar_list.layoutManager.onRestoreInstanceState(
                    this.savedInstanceState!!.getParcelable("CalendarList"))
            val json = this.savedInstanceState!!.getString("CalendarData","")
            calendarListAdapter.setNewData(JsonUtil.toEntity(json, object: TypeToken<List<CalendarAdapter.CalendarSection>>(){}.type))
            this.savedInstanceState = null
        }
        if(calendarListAdapter.data.size == 0){
            loadCalendarList()
        }
    }

    private var calendarCall : Call<List<Calendar>>? = null
    private fun loadCalendarList(){
        calendar_swipe?.isRefreshing = false
        calendarListAdapter.setNewData(null)
        calendarCall?.cancel()
        calendar_swipe?.isRefreshing = true
        calendarCall = api.calendar()
        calendarCall?.enqueue(ApiHelper.buildCallback(calendar_swipe?.context, {
            var index = 0
            it.forEach {
                var isHeader = true
                if(it.weekday?.id == CalendarAdapter.currentWeek())
                    index = calendarListAdapter.data.size
                it.items?.forEach {subject->
                    calendarListAdapter.addData(CalendarAdapter.CalendarSection(isHeader, subject, it.weekday?.id
                            ?: 0))
                    isHeader = false
                }
            }
            (calendar_list?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(index, 0)
        }, { calendar_swipe?.isRefreshing = false }))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_search)?.isVisible = true
        //menu.findItem(R.id.action_type)?.isVisible = false
        super.onPrepareOptionsMenu(menu)
    }
}