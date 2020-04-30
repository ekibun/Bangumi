package soko.ekibun.bangumi.ui.main.fragment.index

import am.util.viewpager.adapter.RecyclePagerAdapter
import android.util.Log
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowInsets
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.content_index.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.view.BaseActivity
import soko.ekibun.bangumi.ui.view.FixSwipeRefreshLayout
import soko.ekibun.bangumi.ui.view.ShadowDecoration
import java.util.*

/**
 * 索引PagerAdapter
 */
class IndexPagerAdapter(val fragment: IndexFragment, pager: androidx.viewpager.widget.ViewPager) :
    RecyclePagerAdapter<IndexPagerAdapter.IndexPagerViewHolder>() {
    var windowInsets: WindowInsets? = null
        set(value) {
            field = value
            holders.forEach {
                it.recyclerView.setPadding(0, 0, 0, windowInsets?.systemWindowInsetBottom ?: 0)
            }
        }

    private val indexTypeView = IndexTypeView(fragment.item_type) {
        pageIndex.clear()
        pager.adapter?.notifyDataSetChanged()
    }

    override fun getPageTitle(pos: Int): CharSequence {
        //TODO
        return "${pos / 12 + 1000}\n${pos % 12 + 1}月"
    }

    private val pageIndex = SparseIntArray()
    private fun loadIndex(item: IndexPagerViewHolder){
        val indexType = IndexTypeView.typeList[indexTypeView.selectedType] ?: return
        val disposeContainer = (fragment.activity as? BaseActivity)?.disposeContainer ?: return

        val position = item.position
        val year = position / 12 + 1000
        val month = position % 12 + 1
        val page = pageIndex.get(position, 0)
        if (page == 0) {
            item.adapter.setNewInstance(null)
            item.view.isRefreshing = true
        }
        item.adapter.isUseEmpty = false

        disposeContainer.subscribeOnUiThread(
            Bangumi.browserAirTime(indexType.first, year, month, page + 1, indexType.second),
            {
                item.adapter.isUseEmpty = true
                if (it.isEmpty()) {
                    item.adapter.loadMoreModule.loadMoreEnd()
                } else {
                    item.adapter.loadMoreModule.loadMoreComplete()
                    item.adapter.addData(it)
                    pageIndex.put(position, (pageIndex.get(position, 0)) + 1)
                }
            }, {
                item.adapter.loadMoreModule.loadMoreFail()
            }, {
                item.view.isRefreshing = false
            },
            key = INDEX_CALL_PREFIX + position
        )
    }

    private fun reset(item: IndexPagerViewHolder){
        val position = item.position
        pageIndex.put(position, 0)
        loadIndex(item)
    }

    private val holders = ArrayList<IndexPagerViewHolder>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndexPagerViewHolder {
        val swipeRefreshLayout = FixSwipeRefreshLayout(parent.context)
        val recyclerView = RecyclerView(parent.context)
        ShadowDecoration.set(recyclerView)
        recyclerView.setPadding(0, 0, 0, windowInsets?.systemWindowInsetBottom ?: 0)
        recyclerView.clipToPadding = false
        val adapter = SubjectAdapter()
        val viewHolder = IndexPagerViewHolder(swipeRefreshLayout, adapter, recyclerView)

        adapter.setEmptyView(LayoutInflater.from(parent.context).inflate(R.layout.view_empty, parent, false))
        adapter.isUseEmpty = false
        adapter.loadMoreModule.setOnLoadMoreListener {
            loadIndex(viewHolder)
        }
        adapter.setOnItemClickListener { _, v, pos ->
            SubjectActivity.startActivity(v.context, adapter.data[pos])
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(parent.context)
        recyclerView.isNestedScrollingEnabled = false
        swipeRefreshLayout.addView(recyclerView)
        swipeRefreshLayout.setOnRefreshListener { reset(viewHolder) }
        holders.add(viewHolder)
        Log.v("holder", holders.size.toString())
        return viewHolder
    }

    override fun getItemCount(): Int {
        return 24000 //2000 years
    }

    override fun onBindViewHolder(holder: IndexPagerViewHolder, position: Int) {
        if(holder.position != position)
            pageIndex.put(position, 0)
        holder.position = position
        if(pageIndex.get(position, 0) == 0)
            loadIndex(holder)
    }

    /**
     * 索引ViewHolder
     * @property view FixSwipeRefreshLayout
     * @property adapter SubjectAdapter
     * @property position Int
     * @constructor
     */
    class IndexPagerViewHolder(
        val view: FixSwipeRefreshLayout,
        val adapter: SubjectAdapter,
        val recyclerView: RecyclerView
    ) :
        RecyclePagerAdapter.PagerViewHolder(view) {
        var position = 0
    }

    companion object {
        private const val INDEX_CALL_PREFIX = "bangumi_index_"
    }
}