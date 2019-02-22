package soko.ekibun.bangumi.ui.search

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType

class SearchAdapter(data: MutableList<Subject>? = null) :
        BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject, data) {

    override fun convert(helper: BaseViewHolder, item: Subject) {
        helper.setText(R.id.item_title, (if(item.collect) " " else "")+ item.getPrettyName())
        helper.setText(R.id.item_name_jp, item.name)
        helper.setText(R.id.item_summary, helper.itemView.context.getString(SubjectType.getDescription(item.type)))
        helper.itemView.item_chase.visibility = if(item.collect) View.VISIBLE else View.GONE
        Glide.with(helper.itemView.item_cover)
                .load(item.images?.getImage(helper.itemView.context))
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(helper.itemView.item_cover)
    }
}
