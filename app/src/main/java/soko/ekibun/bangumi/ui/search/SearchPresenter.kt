package soko.ekibun.bangumi.ui.search

import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.fragment_search.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.ApiHelper.subscribeOnUiThread
import soko.ekibun.bangumi.api.bangumi.Bangumi
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.model.SearchHistoryModel
import soko.ekibun.bangumi.ui.subject.SubjectActivity
import soko.ekibun.bangumi.ui.view.ShadowDecoration
import soko.ekibun.bangumi.ui.web.WebActivity

/**
 * 搜索Presenter
 * @constructor
 */
class SearchPresenter(private val context: SearchActivity) {
    val typeView = SearchTypeView(context.item_type) {
        search(lastKey, true)
    }

    val monoAdapter = MonoAdapter()
    val subjectAdapter = SearchAdapter()
    val searchHistoryAdapter = SearchHistoryAdapter()

    init {
        context.search_history.layoutManager = LinearLayoutManager(context)
        context.search_history.adapter = searchHistoryAdapter
        ShadowDecoration.set(context.search_history)
        val emptyTextView = TextView(context)
        emptyTextView.text = context.getString(R.string.search_hint_no_history)
        emptyTextView.gravity = Gravity.CENTER
        searchHistoryAdapter.setEmptyView(emptyTextView)
        searchHistoryAdapter.setNewInstance(SearchHistoryModel.getHistoryList().toMutableList())
        searchHistoryAdapter.setOnItemClickListener { _, _, position ->
            context.search_box.setText(searchHistoryAdapter.data[position])
            search(context.search_box.text.toString())
        }
        searchHistoryAdapter.setOnItemChildClickListener { _, _, position ->
            SearchHistoryModel.removeHistory(searchHistoryAdapter.data[position])
            searchHistoryAdapter.removeAt(position)
        }
        context.search_history_remove.setOnClickListener {
            AlertDialog.Builder(context).setMessage(R.string.search_dialog_history_clear)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                SearchHistoryModel.clearHistory()
                searchHistoryAdapter.setNewInstance(null)
            }.show()
        }

        ShadowDecoration.set(context.search_list)
        context.search_list.layoutManager = LinearLayoutManager(context)
        subjectAdapter.loadMoreModule.setOnLoadMoreListener { search() }
        subjectAdapter.setOnItemClickListener { _, _, position ->
            SubjectActivity.startActivity(context, subjectAdapter.data[position])
        }
        monoAdapter.loadMoreModule.setOnLoadMoreListener { search() }
        monoAdapter.setOnItemClickListener { _, _, position ->
            WebActivity.launchUrl(context, monoAdapter.data[position].url, "")
        }

        context.search_box.setOnKeyListener { _, keyCode, _ ->
            if(keyCode == KeyEvent.KEYCODE_ENTER) {
                search(context.search_box.text.toString())
                true
            }else false
        }

        context.search_swipe.visibility = View.INVISIBLE
        context.search_box.addTextChangedListener(object: TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) { /* no-op */
            }

            override fun afterTextChanged(s: Editable?) { /* no-op */
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                lastKey = ""
                context.search_swipe.visibility = View.INVISIBLE
                searchHistoryAdapter.setNewInstance(
                    SearchHistoryModel.getHistoryList().filter { s.isEmpty() || it.contains(s) }.toMutableList()
                )
                context.search_history_remove.visibility = if (s.isEmpty()) View.VISIBLE else View.INVISIBLE
            }
        })

        context.search_swipe.setOnRefreshListener {
            search(lastKey, true)
        }
    }

    private var lastKey = ""
    private var loadCount = 0
    /**
     * 搜索
     * @param key String
     * @param refresh Boolean
     */
    fun search(key: String = lastKey, refresh: Boolean = false){
        if(refresh || lastKey != key) {
            lastKey = key
            subjectAdapter.setNewInstance(null)
            monoAdapter.setNewInstance(null)
            loadCount = 0
        }
        if(key.isEmpty()) {
            context.search_swipe?.isRefreshing = false
        }else {
            context.search_swipe.visibility = View.VISIBLE
            SearchHistoryModel.addHistory(key)
            searchHistoryAdapter.setNewInstance(SearchHistoryModel.getHistoryList().toMutableList())

            if (loadCount == 0)
                context.search_swipe?.isRefreshing = true
            val page = loadCount
            if (typeView.subjectTypeList.containsKey(typeView.selectedType)) {
                Bangumi.searchSubject(
                    key, typeView.subjectTypeList[typeView.selectedType]
                        ?: Subject.TYPE_ANY, page + 1
                ).subscribeOnUiThread({ list ->
                    if (list.isEmpty())
                        subjectAdapter.loadMoreModule.loadMoreEnd()
                    else {
                        subjectAdapter.loadMoreModule.loadMoreComplete()
                        subjectAdapter.addData(list)
                        loadCount = page + 1 //searchAdapter.data.size
                    }
                }, {
                    subjectAdapter.loadMoreModule.loadMoreFail()
                }, {
                    context.search_swipe?.isRefreshing = false
                })
                if(context.search_list.adapter != subjectAdapter) context.search_list.adapter = subjectAdapter
            }else {
                Bangumi.searchMono(
                    key, typeView.monoTypeList[typeView.selectedType]
                        ?: "all", page + 1
                ).subscribeOnUiThread({ list ->
                    if (list.isEmpty())
                        monoAdapter.loadMoreModule.loadMoreEnd()
                    else {
                        monoAdapter.loadMoreModule.loadMoreComplete()
                        monoAdapter.addData(list)
                        loadCount = page + 1 //searchAdapter.data.size
                    }
                }, {
                    monoAdapter.loadMoreModule.loadMoreFail()
                }, {
                    context.search_swipe?.isRefreshing = false
                })
                if(context.search_list.adapter != monoAdapter) context.search_list.adapter = monoAdapter
            }
        }
    }
}