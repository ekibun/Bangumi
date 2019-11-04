package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_tag.view.*
import soko.ekibun.bangumi.R

class TagAdapter(data: MutableList<Pair<String, Int>>? = null, var hasTag: (String) -> Boolean = { false }) :
        BaseQuickAdapter<Pair<String, Int>, BaseViewHolder>(R.layout.item_tag, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: Pair<String, Int>) {
        helper.itemView.item_tag_del.visibility = View.GONE
        helper.itemView.item_tag_name.text = item.first
        helper.itemView.isSelected = hasTag(item.first)
        helper.itemView.item_tag_count.visibility = if (item.second > 0) View.VISIBLE else View.GONE
        helper.itemView.item_tag_count.text = "+${item.second}"
    }

    override fun setNewData(data: MutableList<Pair<String, Int>>?) {
        super.setNewData(data?.sortedByDescending {
            hasTag(it.first)
        })
    }
}