package soko.ekibun.bangumi.ui.search

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.MonoInfo
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.SearchHistoryModel
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.web.WebActivity

class SearchPresenter(private val context: SearchActivity) {
    val searchHistoryModel by lazy { SearchHistoryModel(context) }

    val typeView = SearchTypeView(context.item_type) {
        search(lastKey, true)
    }

    val monoAdapter = MonoAdapter()
    val subjectAdapter = SearchAdapter()
    val searchHistoryAdapter = SearchHistoryAdapter()

    init{
        context.search_history.layoutManager = LinearLayoutManager(context)
        context.search_history.adapter = searchHistoryAdapter
        val emptyTextView = TextView(context)
        emptyTextView.text = context.getString(R.string.search_hint_no_history)
        emptyTextView.gravity = Gravity.CENTER
        searchHistoryAdapter.emptyView = emptyTextView
        searchHistoryAdapter.setNewData(searchHistoryModel.getHistoryList())
        searchHistoryAdapter.setOnItemClickListener { _, _, position ->
            context.search_box.setText(searchHistoryAdapter.data[position])
            search(context.search_box.text.toString())
        }
        searchHistoryAdapter.setOnItemChildClickListener { _, _, position ->
            searchHistoryModel.removeHistory(searchHistoryAdapter.data[position])
            searchHistoryAdapter.remove(position)
        }
        context.search_history_remove.setOnClickListener {
            AlertDialog.Builder(context).setMessage(R.string.search_dialog_history_clear).setNegativeButton(R.string.cancel){ _, _ -> }.setPositiveButton(R.string.ok){ _, _ ->
                searchHistoryModel.clearHistory()
                searchHistoryAdapter.setNewData(null)
            }.show()
        }

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

        context.search_box.setOnKeyListener { _, keyCode, _ ->
            if(keyCode == KeyEvent.KEYCODE_ENTER) {
                search(context.search_box.text.toString())
                true
            }else false
        }

        context.search_swipe.visibility = View.INVISIBLE
        context.search_box.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun afterTextChanged(s: Editable?) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                lastKey = ""
                context.search_swipe.visibility = View.INVISIBLE
                searchHistoryAdapter.setNewData(searchHistoryModel.getHistoryList().filter { s.isEmpty() || it.contains(s) })
                context.search_history_remove.visibility = if(s.isEmpty()) View.VISIBLE else View.INVISIBLE
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
            context.search_swipe.visibility = View.VISIBLE
            searchHistoryModel.addHistory(key)
            searchHistoryAdapter.setNewData(searchHistoryModel.getHistoryList())

            if(loadCount == 0)
                context.search_swipe?.isRefreshing = true
            subjectCall?.cancel()
            monoCall?.cancel()
            val page = loadCount
            if(typeView.subjectTypeList.containsKey(typeView.selectedType)){
                subjectCall = Bangumi.searchSubject(key, typeView.subjectTypeList[typeView.selectedType]
                        ?: 0, page + 1)//api.search(key, SubjectType.ALL, loadCount)
                subjectCall?.enqueue(ApiHelper.buildCallback({list->
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
                if(context.search_list.adapter != subjectAdapter) context.search_list.adapter = subjectAdapter
            }else{
                monoCall = Bangumi.searchMono(key, typeView.monoTypeList[typeView.selectedType]
                        ?: "all", page + 1)//api.search(key, SubjectType.ALL, loadCount)
                monoCall?.enqueue(ApiHelper.buildCallback({list->
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
                if(context.search_list.adapter != monoAdapter) context.search_list.adapter = monoAdapter
            }
        }
    }
}