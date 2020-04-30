package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_emoji.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 表情Adapter
 * @constructor
 */
class EmojiAdapter(data: MutableList<Pair<String, String>>? = null) :
        BaseQuickAdapter<Pair<String, String>, BaseViewHolder>(R.layout.item_emoji, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(holder: BaseViewHolder, item: Pair<String, String>) {
        holder.itemView.item_emoji.setOnClickListener {
            setOnItemChildClick(it, holder.layoutPosition)
        }
        GlideUtil.with(holder.itemView.item_emoji)
            ?.load(item.second)
            ?.into(holder.itemView.item_emoji)
    }
}