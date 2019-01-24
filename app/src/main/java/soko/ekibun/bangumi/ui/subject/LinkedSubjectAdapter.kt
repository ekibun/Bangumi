package soko.ekibun.bangumi.ui.subject

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject_small.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject

class LinkedSubjectAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject_small, data) {

    override fun convert(helper: BaseViewHolder, item: Subject) {
        helper.setText(R.id.item_title, item.getPrettyName())
        helper.setText(R.id.item_summary, item.summary)
        Glide.with(helper.itemView.item_cover)
                .load(item.images?.getImage(helper.itemView.context))
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(helper.itemView.item_cover)
    }
}
