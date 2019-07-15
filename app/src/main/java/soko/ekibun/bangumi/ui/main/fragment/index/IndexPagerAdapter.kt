package soko.ekibun.bangumi.ui.main.fragment.index

import am.util.viewpager.adapter.RecyclePagerAdapter
import android.util.Log
import android.util.SparseIntArray
import android.view.LayoutInflater
import android.view.View
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

class IndexPagerAdapter(private val fragment: IndexFragment, private val pager: androidx.viewpager.widget.ViewPager, private val scrollTrigger: (Boolean)->Unit): RecyclePagerAdapter<IndexPagerAdapter.IndexPagerViewHolder>() {
    private val indexTypeView = IndexTypeView(fragment.item_type) {
        pageIndex.clear()
        pager.adapter?.notifyDataSetChanged()
    }

    init{
        pager.addOnPageChangeListener(object: androidx.viewpager.widget.ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                scrollTrigger((holders.firstOrNull { it.position ==  pager.currentItem}?.view?.tag as? androidx.recyclerview.widget.RecyclerView)?.canScrollVertically(-1) == true)
            } })
    }

    override fun getPageTitle(pos: Int): CharSequence{
        //TODO
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
        indexCalls[position]?.enqueue(ApiHelper.buildCallback({
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

    private val holders = ArrayList<IndexPagerViewHolder>()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IndexPagerViewHolder {
        val swipeRefreshLayout = androidx.swiperefreshlayout.widget.SwipeRefreshLayout(parent.context)
        val recyclerView = androidx.recyclerview.widget.RecyclerView(parent.context)
        recyclerView.overScrollMode = View.OVER_SCROLL_NEVER
        val adapter = SubjectAdapter()
        val viewHolder = IndexPagerViewHolder(swipeRefreshLayout, adapter)
        recyclerView.addOnScrollListener(object: androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                if(viewHolder.position == pager.currentItem)
                scrollTrigger(recyclerView.canScrollVertically(-1))
            }
        })

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
        recyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(parent.context)
        recyclerView.isNestedScrollingEnabled = false
        swipeRefreshLayout.addView(recyclerView)
        swipeRefreshLayout.tag = recyclerView
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


    class IndexPagerViewHolder(val view: androidx.swiperefreshlayout.widget.SwipeRefreshLayout, val adapter: SubjectAdapter):
            RecyclePagerAdapter.PagerViewHolder({view}()){
        var position = 0
    }
}