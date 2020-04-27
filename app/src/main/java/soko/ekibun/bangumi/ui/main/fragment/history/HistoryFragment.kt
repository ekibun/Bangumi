package soko.ekibun.bangumi.ui.main.fragment.history

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.content_history.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.model.HistoryModel
import soko.ekibun.bangumi.ui.main.fragment.DrawerFragment

class HistoryFragment : DrawerFragment(R.layout.content_history) {
    override val titleRes: Int = R.string.history

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = HistoryAdapter()
        list_item?.layoutManager = LinearLayoutManager(view.context)
        adapter.setUpWithRecyclerView(shc, list_item)
        adapter.setEmptyView(R.layout.view_empty)
        updateHistory()
        item_swipe?.setOnRefreshListener {
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
                    HistoryModel.removeHistory(adapter.data[position].t!!)
                    updateHistory()
                }.show()
        }
        item_clear_history?.setOnClickListener {
            AlertDialog.Builder(view.context).setMessage(R.string.history_dialog_clear)
                .setNegativeButton(R.string.cancel) { _, _ -> }.setPositiveButton(R.string.ok) { _, _ ->
                    HistoryModel.clearHistory()
                    updateHistory()
                }.show()

        }

        item_swipe?.setOnApplyWindowInsetsListener { _, insets ->
            list_item?.setPadding(0, 0, 0, insets.systemWindowInsetBottom)
            insets.consumeSystemWindowInsets()
        }
    }

    private fun updateHistory() {
        item_swipe?.isRefreshing = false
        val history = ArrayList<HistoryAdapter.History>()
        var dateString = ""
        HistoryModel.historyList.forEach {
            if (dateString != it.dateString) history.add(HistoryAdapter.History(it.dateString))
            history.add(HistoryAdapter.History(it))
            dateString = it.dateString
        }
        (list_item?.adapter as? HistoryAdapter)?.setNewInstance(history)
        item_clear_history?.visibility = if (history.size > 0) View.VISIBLE else View.INVISIBLE
    }
}