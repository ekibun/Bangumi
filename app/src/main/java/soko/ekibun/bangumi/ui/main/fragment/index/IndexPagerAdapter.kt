package soko.ekibun.bangumi.ui.main.fragment.index

import am.util.viewpager.adapter.RecyclePagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.ViewGroup
import kotlinx.android.synthetic.main.content_index.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import java.util.*

class IndexPagerAdapter(private val fragment: IndexFragment, private val pager: ViewPager): RecyclePagerAdapter<IndexPagerAdapter.IndexPagerViewHolder>() {
    private val indexTypeView = IndexTypeView(fragment.item_type) {
        pageIndex.clear()
        pager.adapter?.notifyDataSetChanged()
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return "${pos/12 + 1000}\n${pos%12+1}月"
    }

    private val pageIndex = SparseIntArray()
    private var indexCalls = WeakHashMap<Int, Call<List<Subject>>>()
    private fun loadIndex(item: IndexPagerViewHolder){
        val indexType = IndexTypeView.typeList[indexTypeView.selectedType]?:return

        val position = item.position
        val year = position/12 + 1000
        val month = position % 12 + 1
        val page = pageIndex.get(position,0)
        indexCalls[position]?.cancel()
        if(page == 0){
            item.adapter.setNewData(null)
            item.view.isRefreshing = true
        }
        indexCalls[position] = Bangumi.browserAirTime(indexType.first, year, month, page + 1, (fragment.activity as? MainActivity)?.ua?:"", indexType.second)
        item.adapter.isUseEmpty(false)
        indexCalls[position]?.enqueue(ApiHelper.buildCallback(item.view.context, {
            item.adapter.isUseEmpty(true)
            if(it.isEmpty()){
                item.adapter.loadMoreEnd()
            }else{
                item.adapter.loadMoreComplete()
                item.adapter.addData(it)
                pageIndex.put(position, (pageIndex.get(position,0)) + 1)
            }
        }, {
            item.adapter.loadMoreFail()
            item.view.isRefreshing = false
        }))
    }

    private fun reset(item: IndexPagerViewHolder){
        val position = item.position
        pageIndex.put(position, 0)
        loadIndex(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndexPagerViewHolder {
        val swipeRefreshLayout = SwipeRefreshLayout(parent.context)
        val recyclerView = RecyclerView(parent.context)
        val adapter = SubjectAdapter()
        val viewHolder = IndexPagerViewHolder(swipeRefreshLayout, adapter)
        adapter.emptyView = LayoutInflater.from(parent.context).inflate(R.layout.view_empty, parent, false)
        adapter.isUseEmpty(false)
        adapter.setEnableLoadMore(true)
        adapter.setOnLoadMoreListener({
            loadIndex(viewHolder)
        }, recyclerView)
        adapter.setOnItemClickListener { _, v, pos ->
            SubjectActivity.startActivity(v.context, adapter.data[pos])
        }
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(parent.context)
        recyclerView.isNestedScrollingEnabled = false
        swipeRefreshLayout.addView(recyclerView)
        swipeRefreshLayout.tag = recyclerView
        swipeRefreshLayout.setOnRefreshListener { reset(viewHolder) }

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


    class IndexPagerViewHolder(val view: SwipeRefreshLayout, val adapter: SubjectAdapter):
            RecyclePagerAdapter.PagerViewHolder({view}()){
        var position = 0
    }
}