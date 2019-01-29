package soko.ekibun.bangumi.ui.subject

import android.annotation.SuppressLint
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_tag.view.*
import soko.ekibun.bangumi.R

class EditTagAdapter(data: MutableList<String>? = null) :
        BaseQuickAdapter<String, BaseViewHolder>(R.layout.item_tag, data) {

    @SuppressLint("SetTextI18n")
    override fun convert(helper: BaseViewHolder, item: String) {
        helper.itemView.item_tag_layout.isClickable = false
        helper.itemView.item_tag_layout.isFocusable = false
        helper.itemView.item_tag_name.text = item
        helper.itemView.item_tag_del.setOnClickListener {
            setNewData(data.minus(item))
        }
    }
}