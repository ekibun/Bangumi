package soko.ekibun.bangumi.ui.search

import android.support.v7.widget.LinearLayoutManager
import android.text.Editable
import android.text.TextWatcher
import android.webkit.WebView
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.MonoInfo
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.web.WebActivity

class SearchPresenter(private val context: SearchActivity) {
    val typeView = SearchTypeView(context.item_type) {
        search(lastKey, true)
    }

    val monoAdapter = MonoAdapter()
    val subjectAdapter = SearchAdapter()

    init{
        context.search_list.layoutManager = LinearLayoutManager(context)
        subjectAdapter.setEnableLoadMore(true)
        subjectAdapter.setOnLoadMoreListener({
            search()
        },context.search_list)
        subjectAdapter.setOnItemClickListener { _, _, position ->
            SubjectActivity.startActivity(context, subjectAdapter.data[position])
        }
        monoAdapter.setEnableLoadMore(true)
        monoAdapter.setOnLoadMoreListener({
            search()
        },context.search_list)
        monoAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, monoAdapter.data[position]?.url, "")
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

    private var subjectCall : Call<List<Subject>>? = null
    private var monoCall : Call<List<MonoInfo>>? = null
    private var lastKey = ""
    private var loadCount = 0
    private val ua by lazy { WebView(context).settings.userAgentString }
    fun search(key: String = lastKey, refresh: Boolean = false){
        if(refresh || lastKey != key){
            lastKey = key
            subjectAdapter.setNewData(null)
            monoAdapter.setNewData(null)
            loadCount = 0
        }
        if(key.isEmpty()) {
            context.search_swipe?.isRefreshing = false
            subjectCall?.cancel()
            monoCall?.cancel()
        }else{
            if(loadCount == 0)
                context.search_swipe?.isRefreshing = true
            subjectCall?.cancel()
            monoCall?.cancel()
            val page = loadCount
            context.search_list.adapter = if(typeView.subjectTypeList.containsKey(typeView.selectedType)){
                subjectCall = Bangumi.searchSubject(key, typeView.subjectTypeList[typeView.selectedType]?:0, page+1, ua)//api.search(key, SubjectType.ALL, loadCount)
                subjectCall?.enqueue(ApiHelper.buildCallback(context, {list->
                    //val list =it.list
                    if(list == null || list.isEmpty())
                        subjectAdapter.loadMoreEnd()
                    else{
                        subjectAdapter.loadMoreComplete()
                        subjectAdapter.addData(list)
                        loadCount = page + 1 //searchAdapter.data.size
                    }
                },{
                    subjectAdapter.loadMoreFail()
                    context.search_swipe?.isRefreshing = false
                }))
                subjectAdapter
            }else{
                monoCall = Bangumi.searchMono(key, typeView.monoTypeList[typeView.selectedType]?:"all", page+1, ua)//api.search(key, SubjectType.ALL, loadCount)
                monoCall?.enqueue(ApiHelper.buildCallback(context, {list->
                    //val list =it.list
                    if(list == null || list.isEmpty())
                        monoAdapter.loadMoreEnd()
                    else{
                        monoAdapter.loadMoreComplete()
                        monoAdapter.addData(list)
                        loadCount = page + 1 //searchAdapter.data.size
                    }
                },{
                    monoAdapter.loadMoreFail()
                    context.search_swipe?.isRefreshing = false
                }))
                monoAdapter
            }
        }
    }
}