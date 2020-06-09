package soko.ekibun.bangumi.ui.subject

import android.view.View
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject_small.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 关联条目Adapter
 * @constructor
 */
class LinkedSubjectAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject_small, data) {

    override fun convert(holder: BaseViewHolder, item: Subject) {
        holder.setText(R.id.item_title, item.displayName)
        holder.itemView.item_summary.visibility = if (item.category.isNullOrEmpty()) View.GONE else View.VISIBLE
        holder.setText(R.id.item_summary, item.category)
        GlideUtil.with(holder.itemView.item_cover)
            ?.load(Images.getImage(item.image))
            ?.apply(
                RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder)
                    .transform(CenterCrop(), RoundedCorners(holder.itemView.item_cover.radius))
            )
            ?.into(holder.itemView.item_cover)
    }
}
