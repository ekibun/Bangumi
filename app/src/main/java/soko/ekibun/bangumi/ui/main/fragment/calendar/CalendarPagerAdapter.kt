package soko.ekibun.bangumi.ui.main.fragment.calendar

import am.util.viewpager.adapter.RecyclePagerAdapter
import android.util.SparseArray
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_calendar.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.withContext
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.bean.BangumiCalendarItem
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.MainPresenter
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.ShadowDecoration
import soko.ekibun.bangumi.util.TimeUtil
import java.util.*

/**
 * 时间表PagerAdapter
 * @property view ViewGroup
 * @property windowInsets WindowInsets?
 * @property items SparseArray<CalendarAdapter>
 * @property mainPresenter MainPresenter?
 * @constructor
 */
class CalendarPagerAdapter(private val view: ViewGroup) : RecyclePagerAdapter<CalendarPagerAdapter.PagerViewHolder>() {
    val holders = java.util.ArrayList<PagerViewHolder>()
    private val items = SparseArray<CalendarAdapter>()
    var windowInsets: WindowInsets? = null
        set(value) {
            field = value
            holders.forEach {
                it.recyclerView.setPadding(0, 0, 0, windowInsets?.systemWindowInsetBottom ?: 0)
            }
        }

    init {

        var canScroll = false
        @Suppress("ClickableViewAccessibility")
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
            canScroll || holders.firstOrNull { it.position == view.item_pager.currentItem }?.recyclerView?.canScrollVertically(
                -1
            ) ?: false
        }
        loadCalendarList()
        mainPresenter?.calendar?.let { setOnAirList(it) }
    }

    private fun getItem(position: Int): CalendarAdapter {
        return items.get(position) ?: run {
            val adapter = CalendarAdapter()
            adapter.setOnItemClickListener { _, v, pos ->
                adapter.data[pos].t?.subject?.let { SubjectActivity.startActivity(v.context, it) }
            }
            items.put(position, adapter)
            adapter
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup?, viewType: Int): PagerViewHolder {
        val recyclerView = RecyclerView(view.context)
        ShadowDecoration.set(recyclerView)
        recyclerView.setPadding(0, 0, 0, windowInsets?.systemWindowInsetBottom ?: 0)
        recyclerView.clipToPadding = false
        recyclerView.layoutManager = LinearLayoutManager(view.context)
        recyclerView.isNestedScrollingEnabled = false
        val viewHolder = PagerViewHolder(recyclerView)
        holders.add(viewHolder)
        return viewHolder
    }

    override fun onBindViewHolder(holder: PagerViewHolder, position: Int) {
        holder.recyclerView.adapter = getItem(CalendarAdapter.getCalendarInt(getPostDate(position)))
        holder.position = position
    }

    fun setOnAirList(raw: List<BangumiCalendarItem>): Job? {
        val collectionList = mainPresenter?.collectionList ?: ArrayList()
        return (view.context as? BaseActivity)?.subscribe(key = CALENDAR_COMPUTE_CALL) {
            val list = withContext(Dispatchers.Default) {
                val use30h = App.app.sp.getBoolean("calendar_use_30h", false)
                val now = CalendarAdapter.getNowInt(use30h)

                val onAir = HashMap<Int, ArrayList<CalendarAdapter.CalendarSection>>()
                val calWeek = CalendarAdapter.getIntCalendar(now)
                calWeek.add(Calendar.DAY_OF_MONTH, -7)
                val minDate = CalendarAdapter.getCalendarInt(calWeek)
                calWeek.add(Calendar.DAY_OF_MONTH, +14)
                val maxDate = CalendarAdapter.getCalendarInt(calWeek)
                raw.forEach { subject ->
                    val bangumi = Subject(
                        id = subject.id ?: return@forEach,
                        type = Subject.TYPE_ANIME,
                        name = subject.name,
                        name_cn = subject.name_cn,
                        image = "https://api.bgm.tv/v0/subjects/${subject.id}/image?type=grid",
                        collect = collectionList.find { it.id == subject.id }?.let { Collection() }
                    )
                    subject.eps?.forEach {
                        val dateTime = subject.getEpisodeDateTime(it)
                        if (dateTime.first in minDate..maxDate) onAir.getOrPut(dateTime.first) { ArrayList() }
                            .add(
                                CalendarAdapter.CalendarSection(
                                    CalendarAdapter.OnAir(it, bangumi), dateTime.first, dateTime.second
                                )
                            )
                    }
                }
                onAir.mapValues { entry ->
                    entry.value.sortBy { it.time }
                    val index = if (entry.key == now) entry.value.indexOfLast {
                        CalendarAdapter.pastTime(
                            it.date,
                            it.time,
                            use30h
                        )
                    } + 1 else -1
                    entry.value.forEachIndexed { i, calendarSection ->
                        calendarSection.past = entry.key < now || (entry.key == now && i < index)
                    }
                    if (index >= 0) entry.value.add(index, CalendarAdapter.CalendarSection(true))
                    entry.value to index
                }
            }
            list.forEach { entry ->
                val item = getItem(entry.key)
                val (data, index) = entry.value
                item.setNewInstance(data)
                if (index >= 0) {
                    (holders.firstOrNull { it.position == 7 }?.recyclerView?.layoutManager as? LinearLayoutManager)
                        ?.scrollToPositionWithOffset(index - 1, 0)
                }
            }
        }
    }

    private val mainPresenter: MainPresenter? get() = (view.context as? MainActivity)?.mainPresenter

    /**
     * 加载日历列表
     */
    private fun loadCalendarList() {
        view.item_swipe.isRefreshing = true
        (view.context as? BaseActivity)?.subscribe(onComplete = {
            view.item_swipe.isRefreshing = false
        }, key = MainPresenter.CALENDAR_CALL) {
            mainPresenter?.updateCalendarList()?.let {
                setOnAirList(it)?.join()
            }
        }
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
            CalendarAdapter.getNowInt(App.app.sp.getBoolean("calendar_use_30h", false))
        )
        cal.add(Calendar.DAY_OF_MONTH, pos - 7)
        return cal
    }

    override fun getPageTitle(pos: Int): CharSequence {
        return parseDate(CalendarAdapter.getCalendarInt(getPostDate(pos)))//parseDate(old?.toList()?.sortedBy { it.first }?.get(pos)?.first?:0)
    }

    override fun getItemCount(): Int {
        return 15
    }

    /**
     * ViewHolder
     * @constructor
     */
    class PagerViewHolder(
        val recyclerView: RecyclerView
    ) : RecyclePagerAdapter.PagerViewHolder(recyclerView) {
        var position = 0
    }

    companion object {
        const val CALENDAR_COMPUTE_CALL = "bangumi_calendar_compute"
    }
}