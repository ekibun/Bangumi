package soko.ekibun.bangumi.ui.subject

import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject_small.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.GlideUtil

class LinkedSubjectAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject_small, data) {

    override fun convert(helper: BaseViewHolder, item: Subject) {
        helper.setText(R.id.item_title, item.displayName)
        helper.itemView.item_summary.visibility = if (item.category.isNullOrEmpty()) View.GONE else View.VISIBLE
        helper.setText(R.id.item_summary, item.category)
        GlideUtil.with(helper.itemView.item_cover)
                ?.load(item.images?.getImage(helper.itemView.context))
                ?.apply(RequestOptions.errorOf(R.drawable.err_404))
                ?.into(helper.itemView.item_cover)
    }
}
