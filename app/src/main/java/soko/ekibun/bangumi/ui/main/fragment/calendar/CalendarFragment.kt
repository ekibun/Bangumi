package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.view.Menu
import android.view.View
import com.google.gson.reflect.TypeToken
import com.oushangfeng.pinnedsectionitemdecoration.SmallPinnedHeaderItemDecoration
import kotlinx.android.synthetic.main.content_calendar.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiCallback
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Calendar
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.video.VideoActivity
import soko.ekibun.bangumi.util.JsonUtil

class CalendarFragment: DrawerFragment(R.layout.content_calendar) {
    override val showTab: Boolean = false
    override val titleRes: Int = R.string.calendar


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
                SmallPinnedHeaderItemDecoration.Builder(R.id.item_pinned_header, calendarListAdapter.SECTION_HEADER).create()
        )
        calendarListAdapter.setOnItemChildClickListener { _, v, position ->
            calendarListAdapter.data[position].t?.let {
                VideoActivity.startActivity(v.context, it)
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
        calendarCall?.enqueue(ApiCallback.build(calendar_swipe?.context, {
            it.forEach {
                var isHeader = true
                it.items?.forEach {subject->
                    calendarListAdapter.addData(CalendarAdapter.CalendarSection(isHeader, subject, it.weekday?.id
                            ?: 0))
                    isHeader = false
                }
            }
        }, { calendar_swipe?.isRefreshing = false }))
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        menu.findItem(R.id.action_search)?.isVisible = true
        menu.findItem(R.id.action_type)?.isVisible = false
        super.onPrepareOptionsMenu(menu)
    }
}