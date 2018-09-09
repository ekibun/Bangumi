package soko.ekibun.bangumi.ui.main.fragment.index

import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.SparseIntArray
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.content_index.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection.SubjectTypeView
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import java.util.*

class IndexPagerAdapter(val fragment: IndexFragment, private val pager: ViewPager) : PagerAdapter(){
    private val subjectTypeView = SubjectTypeView(fragment.item_type) { reset() }

    private val pageIndex = SparseIntArray()
    private val items = WeakHashMap<Int, Pair<SubjectAdapter, SwipeRefreshLayout>>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        if(!items.containsKey(position))
            pageIndex.put(position, 0)
        val item = items.getOrPut(position){
            val swipeRefreshLayout = SwipeRefreshLayout(container.context)
            val recyclerView = RecyclerView(container.context)
            val adapter = SubjectAdapter()
            adapter.emptyView = fragment.activity?.layoutInflater?.inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setEnableLoadMore(true)
            adapter.setOnLoadMoreListener({
                loadIndex(position)
            }, recyclerView)
            adapter.setOnItemClickListener { _, v, pos ->
                SubjectActivity.startActivity(v.context, adapter.data[pos])
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener { reset(position) }
            Pair(adapter, swipeRefreshLayout)
        }
        if(pageIndex.get(position, 0) == 0)
            loadIndex(position)
        container.addView(item.second)
        return item.second
    }

    override fun getItemPosition(`object`: Any): Int {
        return PagerAdapter.POSITION_NONE
    }

    private fun reset(){
        pageIndex.clear()
        items.clear()
        pager.adapter?.notifyDataSetChanged()
    }

    private fun reset(position: Int){
        pageIndex.put(position, 0)
        loadIndex(position)
    }

    private var indexCalls = WeakHashMap<Int, Call<List<Subject>>>()
    private fun loadIndex(position: Int = pager.currentItem){
        val year = position/12 + 1000
        val month = position % 12 + 1
        val page = pageIndex.get(position,0)
        val item = items[position]?:return
        indexCalls[position]?.cancel()
        if(page == 0){
            item.first.setNewData(null)
            item.second.isRefreshing = true
        }
        indexCalls[position] = Bangumi.browserAirTime(subjectTypeView.getTypeName(), year, month, page + 1)
        item.first.isUseEmpty(false)
        indexCalls[position]?.enqueue(ApiHelper.buildCallback(fragment.context, {
            item.first.isUseEmpty(true)
            if(it.isEmpty()){
                item.first.loadMoreEnd()
            }else{
                item.first.loadMoreComplete()
                item.first.addData(it)
                pageIndex.put(position, (pageIndex.get(position,0)) + 1)
            }
        }, {
            item.first.loadMoreFail()
            item.second.isRefreshing = false
        }))
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getCount(): Int {
        return 24000
    }

    override fun getPageTitle(pos: Int): CharSequence{
        return "${pos/12 + 1000}\n${pos%12+1}月"
    }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view === `object`
    }
}