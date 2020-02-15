package soko.ekibun.bangumi.ui.main.fragment.home.fragment.timeline

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.bean.TimeLine
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.view.BrvahLoadMoreView
import soko.ekibun.bangumi.ui.view.FixSwipeRefreshLayout

/**
 * 时间线PagerAdapter
 * @property fragment TimeLineFragment
 * @property pager ViewPager
 * @property tabList (kotlin.Array<(kotlin.String..kotlin.String?)>..kotlin.Array<out (kotlin.String..kotlin.String?)>?)
 * @property topicCall HashMap<Int, Call<List<TimeLine>>>
 * @property pageIndex HashMap<Int, Int>
 * @property selectedType Int
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
    private var topicCall = HashMap<Int, Call<List<TimeLine>>>()
    @SuppressLint("UseSparseArrays")
    val pageIndex = HashMap<Int, Int>()

    var selectedType = R.id.timeline_type_friend

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
            recyclerView.overScrollMode = View.OVER_SCROLL_NEVER

            val adapter = TimeLineAdapter()
            adapter.emptyView = LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setEnableLoadMore(true)
            adapter.setLoadMoreView(BrvahLoadMoreView())
            adapter.setOnLoadMoreListener({
                loadTopicList(position)
            }, recyclerView)
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
        item.first.isUseEmpty(false)
        val page = pageIndex.getOrPut(position) { 0 }
        if (page == 0) {
            item.second.isRefreshing = true
            item.first.setNewData(null)
        }
        topicCall[position]?.cancel()
        topicCall[position] = TimeLine.getList(
            listOf(
                "all",
                "say",
                "subject",
                "progress",
                "blog",
                "mono",
                "relation",
                "group",
                "wiki",
                "index",
                "doujin"
            )[position],
            page + 1,
            if (selectedType == R.id.timeline_type_self) (fragment.activity as? MainActivity)?.user else null, selectedType == R.id.timeline_type_all)
        topicCall[position]?.enqueue(ApiHelper.buildCallback({
            item.first.isUseEmpty(true)
            if(it.isEmpty()) item.first.loadMoreEnd()
            else item.first.loadMoreComplete()
            val list = it.toMutableList()
            if(it.isNotEmpty() && item.first.data.lastOrNull { it.isHeader }?.header == it.getOrNull(0)?.header)
                list.removeAt(0)
            item.first.addData(list)
            (item.second.tag as? androidx.recyclerview.widget.RecyclerView)?.tag = true
            pageIndex[position] = (pageIndex[position]?:0) + 1
        },{
            item.second.isRefreshing = false
            item.first.loadMoreFail()
        }))
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

}