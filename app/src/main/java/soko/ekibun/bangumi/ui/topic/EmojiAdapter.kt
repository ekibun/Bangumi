package soko.ekibun.bangumi.ui.topic

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_emoji.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 表情Adapter
 */
class EmojiAdapter(data: MutableList<Pair<String, String>>? = null) :
        BaseQuickAdapter<Pair<String, String>, BaseViewHolder>(R.layout.item_emoji, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: Pair<String, String>) {
        helper.addOnClickListener(R.id.item_emoji)
        GlideUtil.with(helper.itemView.item_emoji)
                ?.load(item.second)
                ?.into(helper.itemView.item_emoji)
    }
}