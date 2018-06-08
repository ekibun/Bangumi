package soko.ekibun.bangumi.ui.search

import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType

class SearchAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject, data) {

    override fun convert(helper: BaseViewHolder, item: Subject) {
        helper.setText(R.id.item_title, if(item.name_cn.isNullOrEmpty()) item.name else item.name_cn)
        helper.setText(R.id.item_name_jp, item.name)
        helper.setText(R.id.item_summary, SubjectType.getDescription(item.type))
        Glide.with(helper.itemView)
                .load(item.images?.common)
                .into(helper.itemView.item_cover)
    }
}
