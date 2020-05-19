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
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Collection
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.UserModel
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.main.MainPresenter
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.FixSwipeRefreshLayout
import soko.ekibun.bangumi.ui.view.ShadowDecoration

/**
 * 收藏PagerAdapter
 * @property context Context
 * @property fragment CollectionFragment
 * @property pager ViewPager
 * @property tabList Array<String>
 * @property collectionTypeView CollectTypeView
 * @property mainPresenter MainPresenter?
 * @property items HashMap<Int, Pair<CollectionListAdapter, FixSwipeRefreshLayout>>
 * @property isScrollDown Boolean
 * @property collectionCalls HashMap<Int, Call<List<Subject>>>
 * @property pageIndex HashMap<Int, Int>
 * @constructor
 */
class CollectionPagerAdapter(
    private val context: Context,
    val fragment: CollectionFragment,
    private val pager: ViewPager
) : androidx.viewpager.widget.PagerAdapter() {
    private val tabList =
        arrayOf(Subject.TYPE_ANIME, Subject.TYPE_BOOK, Subject.TYPE_MUSIC, Subject.TYPE_GAME, Subject.TYPE_REAL)
    val collectionTypeView = CollectTypeView(fragment.item_type) { reset() }
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

    private fun useApi(position: Int): Boolean {
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
            ShadowDecoration.set(recyclerView)
            val adapter = CollectionListAdapter()
            adapter.setEmptyView(LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false))
            adapter.isUseEmpty = false
            adapter.loadMoreModule.setOnLoadMoreListener {
                val useApi = useApi(position)
                if (!swipeRefreshLayout.isRefreshing && !useApi) loadCollectionList(position)
            }
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
        (context as? BaseActivity)?.cancel { it.startsWith(COLLECTION_CALL_PREFIX) }
        loadCollectionList()
    }

    @SuppressLint("UseSparseArrays")
    private val pageIndex = HashMap<Int, Int>()

    fun collectionCallback(list: List<Subject>?, error: Throwable?, fromCache: Boolean = false) {
        items.keys.forEach { position ->
            if (!useApi(position)) return@forEach
            val item = items[position] ?: return@forEach
            item.second.isRefreshing = false
            if (list == null) {
                if (error != null) item.first.loadMoreModule.loadMoreFail()
                return@forEach
            }
            if (!fromCache) item.first.isUseEmpty = true
            list.filter { it.type == tabList[position] }.let {
                item.first.setNewInstance(it.sortedBy { it.airInfo.isNullOrEmpty() }.sortedByDescending {
                    val eps = it.eps?.filter { it.type == Episode.TYPE_MAIN }
                    val watchTo = eps?.lastOrNull { it.progress == Episode.PROGRESS_WATCH }
                    val airTo = eps?.lastOrNull { it.isAir }
                    (if (watchTo != airTo) ":" else "") + (airTo?.airdate ?: "")
                }.toMutableList())
            }
            item.first.loadMoreModule.loadMoreEnd()
            (item.second.tag as? RecyclerView)?.tag = true
            pageIndex[position] = (pageIndex[position] ?: 0) + 1
        }
    }

    private fun loadCollectionList(position: Int = pager.currentItem) {
        val item = items[position] ?: return
        item.second.isRefreshing = false
        item.first.isUseEmpty = false
        val page = pageIndex.getOrPut(position) { 0 }
        val useApi = useApi(position)
        if (page == 0) {
            if (!useApi) item.first.setNewInstance(null)
            else collectionCallback(mainPresenter?.collectionList, null, true)
            item.second.isRefreshing = true
        }
        if (useApi) mainPresenter?.updateUserCollection()
        else {
            (fragment.activity as? BaseActivity)?.subscribe({
                item.second.isRefreshing = false
                item.first.loadMoreModule.loadMoreFail()
            }, COLLECTION_CALL_PREFIX + position) {
                val list = Bangumi.getCollectionList(
                    tabList[position],
                    UserModel.current()?.username ?: throw Exception("login failed"),
                    collectionTypeView.getType(), page + 1
                )
                item.second.isRefreshing = false
                item.first.isUseEmpty = true
                list.forEach { it.type = tabList[position] }
                item.first.addData(list)
                if (list.size < 10)
                    item.first.loadMoreModule.loadMoreEnd()
                else
                    item.first.loadMoreModule.loadMoreComplete()
                (item.second.tag as? RecyclerView)?.tag = true
                pageIndex[position] = (pageIndex[position] ?: 0) + 1
            }
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

    companion object {
        private const val COLLECTION_CALL_PREFIX = "bangumi_collection_"
    }
}