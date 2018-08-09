package soko.ekibun.bangumi.ui.main.fragment.home.fragment.topic

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.support.v4.view.ViewPager
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Topic
import soko.ekibun.bangumi.ui.web.WebActivity

class TopicPagerAdapter(context: Context, val fragment: TopicFragment, private val pager: ViewPager) : PagerAdapter(){
    private val tabList = context.resources.getStringArray(R.array.topic_list)

    private val items = HashMap<Int, Pair<TopicAdapter, SwipeRefreshLayout>>()
    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val item = items.getOrPut(position){
            val swipeRefreshLayout = SwipeRefreshLayout(container.context)
            val recyclerView = RecyclerView(container.context)
            val adapter = TopicAdapter()
            adapter.emptyView = LayoutInflater.from(container.context).inflate(R.layout.view_empty, container, false)
            adapter.isUseEmpty(false)
            adapter.setOnItemClickListener { _, v, position ->
                WebActivity.launchUrl(v.context, adapter.data[position].url)
            }
            recyclerView.adapter = adapter
            recyclerView.layoutManager = LinearLayoutManager(container.context)
            recyclerView.isNestedScrollingEnabled = false
            swipeRefreshLayout.addView(recyclerView)
            swipeRefreshLayout.tag = recyclerView
            swipeRefreshLayout.setOnRefreshListener { loadTopicList(position) }
            Pair(adapter,swipeRefreshLayout)
        }
        container.addView(item.second)
        if((item.second.tag as? RecyclerView)?.tag == null)
            loadTopicList(position)
        return item.second
    }

    private var topicCall = HashMap<Int, Call<List<Topic>>>()
    private fun loadTopicList(position: Int = pager.currentItem){
        val item = items[position]?:return
        item.first.isUseEmpty(false)
        topicCall[position]?.cancel()
        topicCall[position] = Bangumi.getTopics(listOf("", "group", "subject", "ep", "mono")[position])
        item.second.isRefreshing = true
        topicCall[position]?.enqueue(ApiHelper.buildCallback(item.second.context, {
            item.first.isUseEmpty(true)
            item.first.setNewData(it)
            (item.second.tag as? RecyclerView)?.tag = true
        },{
            item.second.isRefreshing = false
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