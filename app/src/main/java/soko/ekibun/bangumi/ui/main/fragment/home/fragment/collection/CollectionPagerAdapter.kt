package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.fragment_collection.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.CollectionStatusType
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.SubjectCollection
import soko.ekibun.bangumi.ui.main.MainActivity
import soko.ekibun.bangumi.ui.subject.SubjectActivity

class CollectionPagerAdapter(context: Context, val fragment: CollectionFragment, private val pager: ViewPager) : PagerAdapter(){
    private val tabList = context.resources.getStringArray(R.array.collection_status)
    private val subjectTypeView = SubjectTypeView(fragment.item_type) { reset() }
    private val api by lazy { Bangumi.createInstance() }

    init{
        pager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener{
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {}
            override fun onPageSelected(position: Int) {
                if((items[position]?.second?.tag as? RecyclerView)?.tag == null) {
                    pageIndex[position] = 0
                    loadCollectionList(position)
                }
            } })
    }

    private val items = HashMap<Int, Pair<CollectionListAdapter, SwipeRefreshLayout>>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = items.getOrPut(position){
            val swipeRefreshLayout = SwipeRefreshLayout(container.context)
            val recyclerView = RecyclerView(container.context)
            val adapter = CollectionListAdapter()
            adapter.emptyView = LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setEnableLoadMore(true)
            adapter.setOnLoadMoreListener({
                loadCollectionList(position)
            }, recyclerView)
            adapter.setOnItemClickListener { _, v, position ->
                SubjectActivity.startActivity(v.context, adapter.data[position].subject!!)
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener { reset() }
            Pair(adapter,swipeRefreshLayout)
        }
        container.addView(item.second)
        if(pageIndex[position] == 0)
            loadCollectionList(position)
        return item.second
    }

    fun reset() {
        items.forEach {  (it.value.second.tag as? RecyclerView)?.tag = null }
        pageIndex.clear()
        loadCollectionList()
    }

    private var collectionCalls = HashMap<Int, Call<List<SubjectCollection>>>()
    private val pageIndex = HashMap<Int, Int>()
    private fun loadCollectionList(position: Int = pager.currentItem){
        val item = items[position]?:return
        item.second.isRefreshing = false
        item.first.isUseEmpty(false)
        val page = pageIndex.getOrPut(position) {0}
        collectionCalls[position]?.cancel()
        if(page == 0)
            item.first.setNewData(null)
        val user =  (fragment.activity as? MainActivity)?.user?:return
        val userName = user.username?:user.id.toString()
        if(page == 0)
            item.second.isRefreshing = true
        val useApi = position == 2 && subjectTypeView.selectedType in arrayOf(R.id.collection_type_anime, R.id.collection_type_book, R.id.collection_type_real)
        collectionCalls[position] = if(useApi) Bangumi.getCollection()//api.collection(userName)
        else Bangumi.getCollectionList(subjectTypeView.getTypeName(), userName, CollectionStatusType.status[position], page+1)
        collectionCalls[position]?.enqueue(ApiHelper.buildCallback(item.second.context, {
            item.first.isUseEmpty(true)
            it.filter { !useApi || it.subject?.type == subjectTypeView.getType() }.let{
                if(!useApi) it.forEach {
                    it.subject?.type = subjectTypeView.getType() }
                item.first.addData(it.sortedByDescending { (it.subject?.eps as? List<*>)?.mapNotNull { it as? Episode }?.lastOrNull { it.status == "Air" }?.airdate }) }
            if(useApi || it.size < 10)
                item.first.loadMoreEnd()
            else
                item.first.loadMoreComplete()
            (item.second.tag as? RecyclerView)?.tag = true
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