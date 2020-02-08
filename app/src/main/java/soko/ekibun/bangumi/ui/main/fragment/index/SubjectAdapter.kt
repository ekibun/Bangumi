package soko.ekibun.bangumi.ui.main.fragment.index

import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.GlideUtil

/**
 * 索引条目Adapter
 */
class SubjectAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject, data) {

    override fun convert(helper: BaseViewHolder, item: Subject) {
        helper.setText(R.id.item_title, item.displayName)
        helper.setText(R.id.item_name_jp, item.name)
        helper.setText(R.id.item_summary, item.summary)
        helper.itemView.item_chase.visibility = if (item.collect != null) View.VISIBLE else View.GONE
        GlideUtil.with(helper.itemView.item_cover)
            ?.load(Images.getImage(item.image, helper.itemView.context))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
                ?.into(helper.itemView.item_cover)
    }
}