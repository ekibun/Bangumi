package soko.ekibun.bangumi.ui.search

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_search_history.view.*
import soko.ekibun.bangumi.R

/**
 * 搜索历史Adapter
 * @constructor
 */
class SearchHistoryAdapter(data: MutableList<String>? = null) :
        BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_search_history, data) {

    override fun convert(holder: BaseViewHolder, item: String) {
        holder.setText(R.id.item_search_key, item)
        holder.itemView.item_remove_key.setOnClickListener {
            setOnItemChildClick(it, holder.layoutPosition)
        }
    }
}
