package soko.ekibun.bangumi.ui.main.fragment.calendar

import android.annotation.SuppressLint
import android.content.SharedPreferences
import android.util.SparseArray
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_calendar.view.*
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.MainPresenter
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.view.ShadowDecoration
import soko.ekibun.bangumi.util.TimeUtil
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

/**
 * 时间表PagerAdapter
 * @property view ViewGroup
 * @property sp SharedPreferences
 * @property windowInsets WindowInsets?
 * @property items SparseArray<CalendarAdapter>
 * @property dataCacheModel DataCacheModel
 * @property currentView RecyclerView?
 * @property mainPresenter MainPresenter?
 * @constructor
 */
@SuppressLint("ClickableViewAccessibility")
class CalendarPagerAdapter(private val view: ViewGroup) : androidx.viewpager.widget.PagerAdapter() {
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(view.context) }
    var windowInsets: WindowInsets? = null
    private val items = SparseArray<CalendarAdapter>()

    private val dataCacheModel by lazy { App.app.dataCacheModel }

    init {

        var canScroll = false
        view.item_pager.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_MOVE -> {
                    canScroll = true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    canScroll = false
                }
            }
            false
        }

        view.item_swipe.setOnRefreshListener {
            loadCalendarList()
        }
        view.item_swipe.setOnChildScrollUpCallback { _, _ ->
            canScroll || currentView?.canScrollVertically(-1) ?: false
        }
        mainPresenter?.calendar?.let { setOnAirList(it) }
        loadCalendarList()
    }

    private fun getItem(position: Int): CalendarAdapter {
        return items.get(position) ?: {
            val adapter = CalendarAdapter()
            adapter.setOnItemChildClickListener { _, v, pos ->
                SubjectActivity.startActivity(v.context, adapter.data[pos].t.subject)
            }
            items.put(position, adapter)
            adapter
        }()
    }

    private val currentView: RecyclerView? get() = view.item_pager.findViewWithTag(view.item_pager.currentItem)
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val adapter = getItem(CalendarAdapter.getCalendarInt(getPostDate(position)))
        val recyclerView = RecyclerView(view.context)
        ShadowDecoration.set(recyclerView)
        recyclerView.setPadding(0, 0, 0, windowInsets?.systemWindowInsetBottom ?: 0)
        recyclerView.clipToPadding = false

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.isNestedScrollingEnabled = false
        recyclerView.tag = position

        container.addView(recyclerView)
        return recyclerView
    }

    fun setOnAirList(it: List<BangumiCalendarItem>) {
        val use30h = sp.getBoolean("calendar_use_30h", false)

        val now = CalendarAdapter.getNowInt(use30h)
        @SuppressLint("UseSparseArrays")
        val onAir = HashMap<Int, HashMap<String, ArrayList<CalendarAdapter.OnAir>>>()
        val calWeek = CalendarAdapter.getIntCalendar(now)
        calWeek.add(Calendar.DAY_OF_MONTH, -7)
        val minDate = CalendarAdapter.getCalendarInt(calWeek)
        calWeek.add(Calendar.DAY_OF_MONTH, +14)
        val maxDate = CalendarAdapter.getCalendarInt(calWeek)
        it.forEach { subject ->
            val bangumi = Subject(
                id = subject.id ?: return@forEach,
                type = Subject.TYPE_ANIME,
                name = subject.name,
                name_cn = subject.name_cn,
                image = subject.image
            )
            subject.eps?.forEach {
                val item = CalendarAdapter.OnAir(it, bangumi)
                val dateTime = subject.getEpisodeDateTime(it)
                if (dateTime.first in minDate..maxDate) onAir.getOrPut(dateTime.first) { HashMap() }
                    .getOrPut(dateTime.second) { ArrayList() }.add(item)
            }
        }
        onAir.toList().forEach { date ->
            var index = -1
            val item = getItem(date.first)
            val data = ArrayList<CalendarAdapter.CalendarSection>()
            date.second.toList().sortedBy { it.first }.forEach { time ->
                var isHeader = true
                time.second.forEach {
                    it.subject.collect =
                        if (mainPresenter?.collectionList?.find { c -> c.id == it.subject.id } != null)
                            Collection(Collection.STATUS_DO) else null
                    if (it.subject.image != null) {
                        if (index == -1 && !CalendarAdapter.pastTime(date.first, time.first, use30h))
                            index = item.data.size
                        data.add(CalendarAdapter.CalendarSection(isHeader, it, date.first, time.first))
                        isHeader = false
                    }
                }
            }
            item.setNewData(data)
            if (date.first == now) {
                ((view.item_pager.findViewWithTag(7) as? RecyclerView)?.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(
                    index - 1,
                    0
                )
            }
        }
    }

    private val mainPresenter: MainPresenter? get() = (view.context as? MainActivity)?.mainPresenter

    /**
     * 加载日历列表
     */
    private fun loadCalendarList() {
        view.item_swipe.isRefreshing = true
        mainPresenter?.updateCalendarList()
    }

    fun calendarCallback(data: List<BangumiCalendarItem>?, error: Throwable?) {
        if (data == null) {
            view.item_swipe.isRefreshing = false
            return
        }
        setOnAirList(data)
    }

    override fun getItemPosition(`object`: Any): Int {
        return POSITION_NONE
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    private fun parseDate(date: Int): String {
        val cal = CalendarAdapter.getIntCalendar(date)
        return "${date / 100 % 100}-${date % 100
        }\n${TimeUtil.weekList[CalendarAdapter.getWeek(cal)]
        }(${TimeUtil.weekJp[CalendarAdapter.getWeek(cal)]})"
    }

    /**
     * 获取项对应的时间
     * @param pos Int
     * @return Calendar
     */
    fun getPostDate(pos: Int): Calendar {
        val cal = CalendarAdapter.getIntCalendar(
            CalendarAdapter.getNowInt(sp.getBoolean("calendar_use_30h", false))
        )
        cal.add(Calendar.DAY_OF_MONTH, pos - 7)
        return cal
    }

    override fun getPageTitle(pos: Int): CharSequence {
        return parseDate(CalendarAdapter.getCalendarInt(getPostDate(pos)))//parseDate(old?.toList()?.sortedBy { it.first }?.get(pos)?.first?:0)
    }

    override fun getCount(): Int {
        return 15
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}