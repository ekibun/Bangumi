package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import kotlinx.android.synthetic.main.fragment_collection.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.MainPresenter
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.view.BrvahLoadMoreView
import soko.ekibun.bangumi.ui.view.FixSwipeRefreshLayout

/**
 * 收藏PagerAdapter
 */
class CollectionPagerAdapter(
    private val context: Context,
    val fragment: CollectionFragment,
    private val pager: ViewPager
) : androidx.viewpager.widget.PagerAdapter() {
    private val tabList =
        arrayOf(Subject.TYPE_ANIME, Subject.TYPE_BOOK, Subject.TYPE_MUSIC, Subject.TYPE_GAME, Subject.TYPE_REAL)
    private val collectionTypeView = CollectTypeView(fragment.item_type) { reset() }
    private val mainPresenter: MainPresenter? get() = (fragment.activity as? MainActivity)?.mainPresenter

    init {
        pager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) { /* no-op */
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) { /* no-op */
            }

            override fun onPageSelected(position: Int) {
                if ((items[position]?.second?.tag as? RecyclerView)?.tag == null) {
                    pageIndex[position] = 0
                    loadCollectionList(position)
                }
            }
        })
    }

    @SuppressLint("UseSparseArrays")
    private val items = HashMap<Int, Pair<CollectionListAdapter, FixSwipeRefreshLayout>>()

    val isScrollDown get() = (items[pager.currentItem]?.second?.tag as? RecyclerView)?.canScrollVertically(-1) == true

    fun useApi(position: Int): Boolean {
        return collectionTypeView.getType() == Collection.STATUS_DO && tabList[position] in arrayOf(
            Subject.TYPE_ANIME,
            Subject.TYPE_BOOK,
            Subject.TYPE_REAL
        )
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = items.getOrPut(position) {
            val swipeRefreshLayout = FixSwipeRefreshLayout(container.context)
            val recyclerView = RecyclerView(container.context)

            val adapter = CollectionListAdapter()
            adapter.emptyView = LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setEnableLoadMore(true)
            adapter.setLoadMoreView(BrvahLoadMoreView())
            adapter.setOnLoadMoreListener({
                val useApi = useApi(position)
                if (!swipeRefreshLayout.isRefreshing && !useApi) loadCollectionList(position)
            }, recyclerView)
            adapter.setOnItemClickListener { _, v, position ->
                SubjectActivity.startActivity(v.context, adapter.data[position])
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener { reset() }
            Pair(adapter, swipeRefreshLayout)
        }
        container.addView(item.second)
        if (pageIndex[position] == 0)
            loadCollectionList(position)
        return item.second
    }

    /**
     * 重置
     */
    fun reset() {
        items.forEach { (it.value.second.tag as? RecyclerView)?.tag = null }
        pageIndex.clear()
        loadCollectionList()
    }

    @SuppressLint("UseSparseArrays")
    private var collectionCalls = HashMap<Int, Call<List<Subject>>>()
    @SuppressLint("UseSparseArrays")
    private val pageIndex = HashMap<Int, Int>()

    private fun loadCollectionList(position: Int = pager.currentItem) {
        val item = items[position] ?: return
        item.second.isRefreshing = false
        item.first.isUseEmpty(false)
        val page = pageIndex.getOrPut(position) { 0 }
        collectionCalls[position]?.cancel()
        val useApi = useApi(position)
        if (page == 0) {
            if (!useApi) item.first.setNewData(null)
            else mainPresenter?.collectionList?.filter { it.type == tabList[position] }?.let {
                item.first.setNewData(it.sortedByDescending {
                    val eps = it.eps?.filter { it.type == Episode.TYPE_MAIN }
                    val watchTo = eps?.lastOrNull { it.progress == Episode.PROGRESS_WATCH }
                    val airTo = eps?.lastOrNull { it.isAir }
                    (if (watchTo != airTo) ":" else "") + (airTo?.airdate ?: "")
                }.toMutableList())
            }
            item.second.isRefreshing = true
        }
        val callback = { list: List<Subject> ->
            item.first.isUseEmpty(true)
            list.filter { !useApi || it.type == tabList[position] }.let {
                if (!useApi) {
                    it.forEach { it.type = tabList[position] }
                    item.first.addData(it)
                } else {
                    item.first.setNewData(it.sortedByDescending {
                        val eps = it.eps?.filter { it.type == Episode.TYPE_MAIN }
                        val watchTo = eps?.lastOrNull { it.progress == Episode.PROGRESS_WATCH }
                        val airTo = eps?.lastOrNull { it.isAir }
                        (if (watchTo != airTo) ":" else "") + (airTo?.airdate ?: "")
                    }.toMutableList())
                }
            }
            if (useApi || list.size < 10)
                item.first.loadMoreEnd()
            else
                item.first.loadMoreComplete()
            (item.second.tag as? RecyclerView)?.tag = true
            pageIndex[position] = (pageIndex[position] ?: 0) + 1
        }
        val onError = { it: Throwable? ->
            item.second.isRefreshing = false
            if (it != null) item.first.loadMoreFail()
        }
        if (useApi) mainPresenter?.updateUserCollection({
            if (collectionTypeView.getType() == Collection.STATUS_DO) callback(it)
        }, {
            if (collectionTypeView.getType() == Collection.STATUS_DO) onError(it)
        })
        else {
            collectionCalls[position] = Bangumi.getCollectionList(tabList[position],
                (fragment.activity as? MainActivity)?.user?.let { it.username ?: it.id.toString() } ?: return,
                collectionTypeView.getType(), page + 1)
            collectionCalls[position]?.enqueue(ApiHelper.buildCallback(callback, onError))
        }
    }

    override fun getPageTitle(pos: Int): CharSequence {
        return context.getString(Subject.getTypeRes(tabList[pos]))
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