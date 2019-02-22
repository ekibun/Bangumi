package soko.ekibun.bangumi.ui.search

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import soko.ekibun.bangumi.R

class SearchHistoryAdapter(data: MutableList<String>? = null) :
        BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_search_history, data) {

    override fun convert(helper: BaseViewHolder, item: String) {
        helper.setText(R.id.item_search_key, item)
        helper.addOnClickListener(R.id.item_remove_key)
    }
}
