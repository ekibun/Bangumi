package soko.ekibun.bangumi.ui.main.fragment.collection

import com.bumptech.glide.Glide
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.SubjectCollection

class CollectionListAdapter(data: MutableList<SubjectCollection>? = null) :
        BaseQuickAdapter<SubjectCollection, BaseViewHolder>(R.layout.item_subject, data) {

    override fun convert(helper: BaseViewHolder, item: SubjectCollection) {
        helper.setText(R.id.item_title, item.subject?.name_cn)
        helper.setText(R.id.item_name_jp, item.subject?.name)
        val status = Math.max(item.ep_status, item.vol_status)
        helper.setText(R.id.item_summary, if(status == -1) item.subject?.summary else parseDesc(status))
        Glide.with(helper.itemView)
                .load(item.subject?.images?.common)
                .into(helper.itemView.item_cover)
    }

    private fun parseDesc(lastView: Int): String {
        return if (lastView == 0) "尚未观看" else "看到第 $lastView 话"
    }
}