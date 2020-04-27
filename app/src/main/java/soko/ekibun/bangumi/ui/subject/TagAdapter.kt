package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import android.view.View
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_tag.view.*
import soko.ekibun.bangumi.R

/**
 * 标签Adapter
 * @property hasTag Function1<String, Boolean>
 * @constructor
 */
class TagAdapter(data: MutableList<Pair<String, Int>>? = null, var hasTag: (String) -> Boolean = { false }) :
        BaseQuickAdapter<Pair<String, Int>, BaseViewHolder>(R.layout.item_tag, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: Pair<String, Int>) {
        holder.itemView.item_tag_del.visibility = View.GONE
        holder.itemView.item_tag_name.text = item.first
        holder.itemView.isSelected = hasTag(item.first)
        holder.itemView.item_tag_count.visibility = if (item.second > 0) View.VISIBLE else View.GONE
        holder.itemView.item_tag_count.text = "+${item.second}"
    }

    override fun setNewInstance(data: MutableList<Pair<String, Int>>?) {
        super.setNewInstance(data?.sortedByDescending {
            hasTag(it.first)
        }?.toMutableList())
    }
}