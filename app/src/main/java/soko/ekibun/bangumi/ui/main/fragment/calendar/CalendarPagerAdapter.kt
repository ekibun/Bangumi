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
import retrofit2.Call
import soko.ekibun.bangumi.App
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.github.Jsdelivr
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
 * @property calendarCall Call<List<BangumiCalendarItem>>?
 * @constructor
 */
@SuppressLint("ClickableViewAccessibility")
class CalendarPagerAdapter(private val view: ViewGroup) : androidx.viewpager.widget.PagerAdapter() {
    private val sp: SharedPreferences by lazy { PreferenceManager.getDefaultSharedPreferences(view.context) }
    var windowInsets: WindowInsets? = null
    private val items = SparseArray<CalendarAdapter>()

    private val dataCacheModel by lazy { App.get(view.context).dataCacheModel }

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
        val useCN = sp.getBoolean("calendar_use_cn", false)
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
            val useCNTime = useCN && !subject.timeCN.isNullOrEmpty() // 判断是否使用国内时间
            val timeInt = (if (useCNTime) subject.timeCN else subject.timeJP)?.toIntOrNull() ?: 0
            val weekInt = (if (useCNTime) subject.weekDayCN else subject.weekDayJP) ?: 0

            val bangumi = Subject(
                id = subject.id ?: return@forEach,
                type = Subject.TYPE_ANIME,
                name = subject.name,
                name_cn = subject.name_cn,
                image = subject.image
            )
            subject.eps?.forEach {
                val item = CalendarAdapter.OnAir(it, bangumi)
                val zoneOffset = TimeZone.getDefault().rawOffset / 1000 / 60    // 时差（min）
                val hourDif = zoneOffset / 60 - 8           // 小时差（源数据是UTC+8，减8）
                val minuteDif = zoneOffset % 60             // 分钟差
                val minute = timeInt % 100 + minuteDif      // 分钟 + 分钟差
                val hour = timeInt / 100 + hourDif + when { // 小时 + 小时差 + 分钟的进位
                    minute >= 60 -> 1   // 大于60，进1位
                    minute < 0 -> -1    // 小于0，退1位
                    else -> 0
                }
                val dayCarry = when {               // 日期进位
                    hour >= if (use30h) 30 else 24 -> 1  // 30小时制大于30进位，否则大于24进位
                    hour < if (use30h) 6 else 0 -> -1    // 30小时制小于6退位，否则小于0退位
                    else -> 0
                }
                // 格式化日期 -> hh:mm
                val time = String.format(
                    "%02d:%02d",
                    if (use30h) (hour - 6 + 24) % 24 + 6 else (hour + 24) % 24, minute % 60
                )
                // 根据airdate创建日期对象
                val cal = Calendar.getInstance()
                cal.time = try {
                    TimeUtil.dateFormat.parse(it.airdate ?: "")
                } catch (e: Exception) {
                    null
                } ?: cal.time
                val dayDif = dayCarry + if (
                    timeInt / 100 < (if (useCNTime) 5 else 6) && // 假设airdate按30小时算，且国内放送时间与日本相同，若日本放送时间<6:00，取次日
                    (CalendarAdapter.getWeek(cal) - weekInt + 7) % 7 > 0 // 若日本放送星期与airdate相同，取0
                ) 1 else 0
                cal.add(Calendar.DAY_OF_MONTH, dayDif)   // 加上日期差，计算日期
                val date = CalendarAdapter.getCalendarInt(cal)
                if (date in minDate..maxDate) onAir.getOrPut(date) { HashMap() }.getOrPut(time) { ArrayList() }.add(item)
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
    private var calendarCall: Call<List<BangumiCalendarItem>>? = null
    /**
     * 加载日历列表
     */
    @SuppressLint("UseSparseArrays")
    fun loadCalendarList() {
        view.item_swipe.isRefreshing = true

        calendarCall?.cancel()
        calendarCall = Jsdelivr.createInstance().bangumiCalendar()
        calendarCall?.enqueue(ApiHelper.buildCallback({
            mainPresenter?.calendar = it
            setOnAirList(it ?: return@buildCallback)
            dataCacheModel.set<List<BangumiCalendarItem>>("calendar", it)
        }, {
            view.item_swipe.isRefreshing = false
        }))
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