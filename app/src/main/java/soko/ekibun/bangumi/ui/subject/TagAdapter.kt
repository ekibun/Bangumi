package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_tag.view.*
import soko.ekibun.bangumi.R

class TagAdapter(data: MutableList<Pair<String, Int>>? = null) :
        BaseQuickAdapter<Pair<String, Int>, BaseViewHolder>(R.layout.item_tag, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: Pair<String, Int>) {
        helper.itemView.item_tag_name.text = item.first
        helper.itemView.item_tag_count.text = "+${item.second}"
    }
}