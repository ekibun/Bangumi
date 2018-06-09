package soko.ekibun.bangumi.ui.search

import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
//import android.util.Log
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.SearchResult
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.ui.video.VideoActivity

//import soko.ekibun.bangumi.ui.subject.SubjectActivity

class SearchPresenter(private val context: SearchActivity) {
    val api by lazy { Bangumi.createInstance() }

    private val searchAdapter = SearchAdapter()

    init{
        context.search_list.adapter = searchAdapter
        context.search_list.layoutManager = LinearLayoutManager(context)
        searchAdapter.setEnableLoadMore(true)
        searchAdapter.setOnLoadMoreListener({
            search()
        },context.search_list)
        searchAdapter.setOnItemClickListener { _, _, position ->
            VideoActivity.startActivity(context, searchAdapter.data[position])
        }

        context.search_box.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                search(s.toString())
            }
        })

        context.search_swipe.setOnRefreshListener {
            search(lastKey, true)
        }
    }

    private var searchCall : Call<SearchResult>? = null
    private var lastKey = ""
    private var loadCount = 0
    fun search(key: String = lastKey, refresh: Boolean = false){
        if(refresh || lastKey != key){
            lastKey = key
            searchAdapter.setNewData(null)
            loadCount = 0
        }
        if(key.isEmpty()) {
            context.search_swipe?.isRefreshing = false
            searchCall?.cancel()
        }else{
            if(loadCount == 0)
                context.search_swipe?.isRefreshing = true
            searchCall?.cancel()
            searchCall = api.search(key, SubjectType.ALL, loadCount)
            searchCall?.enqueue(ApiHelper.buildCallback(context, {
                val list =it.list
                if(list == null)
                    searchAdapter.loadMoreEnd()
                else{
                    searchAdapter.loadMoreComplete()
                    searchAdapter.addData(list)
                    loadCount = searchAdapter.data.size
                }
            },{
                searchAdapter.loadMoreFail()
                context.search_swipe?.isRefreshing = false
            }))
        }
    }
}