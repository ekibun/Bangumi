package soko.ekibun.bangumi.ui.main.fragment.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_history.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment
import soko.ekibun.bangumi.ui.view.BaseActivity

class HistoryFragment : DrawerFragment(R.layout.content_history) {
    override val titleRes: Int = R.string.history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = HistoryAdapter()
        list_item?.layoutManager = LinearLayoutManager(view.context)
        adapter.setUpWithRecyclerView(shc, list_item)
        adapter.setEmptyView(R.layout.view_empty)
        updateHistory(0)
        item_swipe?.setOnRefreshListener {
            updateHistory(0)
        }
        adapter.loadMoreModule.setOnLoadMoreListener {
            updateHistory()
        }
        adapter.setOnItemClickListener { _, _, position ->
            adapter.data[position].let {
                if (!it.isHeader) adapter.data[position].t?.startActivity(view.context)
            }
        }

        adapter.setOnItemChildClickListener { _, _, position ->
            AlertDialog.Builder(view.context).setMessage(R.string.history_dialog_remove)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                    (activity as BaseActivity).disposeContainer.subscribeOnUiThread(
                        HistoryModel.removeHistory(adapter.data[position].t!!).toObservable<Unit>(), {},
                        onComplete = {
                            updateHistory(0)
                        }
                    )
                }.show()
        }
        item_clear_history?.setOnClickListener {
            AlertDialog.Builder(view.context).setMessage(R.string.history_dialog_clear)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                    (activity as BaseActivity).disposeContainer.subscribeOnUiThread(
                        HistoryModel.clearHistory().toObservable<Unit>(), {},
                        onComplete = {
                            updateHistory(0)
                        }
                    )
                }.show()

        }

        item_swipe?.setOnApplyWindowInsetsListener { _, insets ->
            list_item?.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }
    }

    var curpage = 0
    private fun updateHistory(page: Int = curpage) {
        curpage = page
        val adapter = (list_item?.adapter as? HistoryAdapter) ?: return
        if (page == 0) {
            adapter.isUseEmpty = false
            adapter.setNewInstance(null)
        }
        item_clear_history?.visibility = if (adapter.data.size > 0) View.VISIBLE else View.INVISIBLE
        (activity as? BaseActivity)?.disposeContainer?.subscribeOnUiThread(
            HistoryModel.getHistoryList(page).toObservable(),
            { list ->
                var dateString = adapter.data.lastOrNull { it.t != null }?.t?.dateString
                list.forEach {
                    if (dateString != it.dateString) adapter.addData(HistoryAdapter.HistorySection(it.dateString))
                    adapter.addData(HistoryAdapter.HistorySection(it))
                    dateString = it.dateString
                }
                adapter.isUseEmpty = true
                if (adapter.data.isEmpty()) adapter.notifyDataSetChanged()
                item_swipe?.isRefreshing = false
                item_clear_history?.visibility = if (adapter.data.size > 0) View.VISIBLE else View.INVISIBLE
                if (list.isEmpty()) adapter.loadMoreModule.loadMoreEnd()
                else adapter.loadMoreModule.loadMoreComplete()
                curpage++
            },
            key = HISTORY_LIST_CALL
        )
    }

    companion object {
        const val HISTORY_LIST_CALL = "history_list_call"
    }
}