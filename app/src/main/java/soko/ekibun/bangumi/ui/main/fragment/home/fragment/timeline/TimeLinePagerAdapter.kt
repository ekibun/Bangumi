package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.TimeLine
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.FixSwipeRefreshLayout
import soko.ekibun.bangumi.ui.view.ShadowDecoration

/**
 * 时间线PagerAdapter
 * @property fragment TimeLineFragment
 * @property pager ViewPager
 * @property tabList (kotlin.Array<(kotlin.String..kotlin.String?)>..kotlin.Array<out (kotlin.String..kotlin.String?)>?)
 * @property pageIndex HashMap<Int, Int>
 * @property items HashMap<Int, Pair<TimeLineAdapter, FixSwipeRefreshLayout>>
 * @constructor
 */
class TimeLinePagerAdapter(
    context: Context,
    val fragment: TimeLineFragment,
    private val pager: androidx.viewpager.widget.ViewPager
) : androidx.viewpager.widget.PagerAdapter() {
    private val tabList = context.resources.getStringArray(R.array.timeline_list)
    @SuppressLint("UseSparseArrays")
    val pageIndex = HashMap<Int, Int>()

    init {
        pager.addOnPageChangeListener(object : androidx.viewpager.widget.ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                if ((items[position]?.second?.tag as? androidx.recyclerview.widget.RecyclerView)?.tag == null) {
                    pageIndex[position] = 0
                    loadTopicList(position)
                }
            }
        })
    }

    /**
     * 重置
     */
    fun reset() {
        items.forEach { (it.value.second.tag as? androidx.recyclerview.widget.RecyclerView)?.tag = null }
        pageIndex.clear()
        loadTopicList()
    }

    @SuppressLint("UseSparseArrays")
    private val items = HashMap<Int, Pair<TimeLineAdapter, FixSwipeRefreshLayout>>()

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = items.getOrPut(position) {
            val swipeRefreshLayout = FixSwipeRefreshLayout(container.context)
            val recyclerView = androidx.recyclerview.widget.RecyclerView(container.context)
            ShadowDecoration.set(recyclerView)
            val adapter = TimeLineAdapter()
            adapter.setEmptyView(LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false))
            adapter.isUseEmpty = false
            adapter.loadMoreModule.setOnLoadMoreListener {
                loadTopicList(position)
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener {
                pageIndex[position] = 0
                loadTopicList(position)
            }
            Pair(adapter, swipeRefreshLayout)
        }
        container.addView(item.second)
        if (pageIndex[position] == 0)
            loadTopicList(position)
        return item.second
    }

    /**
     * 加载帖子列表
     * @param position Int
     */
    fun loadTopicList(position: Int = pager.currentItem) {
        val item = items[position] ?: return
        item.first.isUseEmpty = false
        val page = pageIndex.getOrPut(position) { 0 }
        if (page == 0) {
            item.second.isRefreshing = true
        }
        (fragment.activity as? BaseActivity)?.subscribe(onComplete = {
            item.second.isRefreshing = false
        }, key = TIMELINE_CALL_PREFIX + position) {
            val list = TimeLine.getList(
                listOf(
                    "all", "say", "subject", "progress", "blog", "mono",
                    "relation", "group", "wiki", "index", "doujin"
                )[position],
                page + 1,
                if (fragment.selectedType == R.id.timeline_type_self) UserModel.current() else null,
                fragment.selectedType == R.id.timeline_type_all
            ).toMutableList()
            if (list.isNotEmpty() && item.first.data.lastOrNull { it.isHeader }?.header == list.getOrNull(0)?.header)
                list.removeAt(0)
            if (page == 0) item.first.setNewInstance(list)
            else item.first.addData(list)
            if (list.isEmpty() || fragment.selectedType == R.id.timeline_type_all) item.first.loadMoreModule.loadMoreEnd()
            else item.first.loadMoreModule.loadMoreComplete()
            (item.second.tag as? androidx.recyclerview.widget.RecyclerView)?.tag = true
            pageIndex[position] = (pageIndex[position] ?: 0) + 1
        }
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return tabList[pos]
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }

    override fun getCount(): Int {
        return tabList.size
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    companion object {
        private const val TIMELINE_CALL_PREFIX = "bangumi_timeline_"
    }
}