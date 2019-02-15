package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.SubjectCollection
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.util.ResourceUtil

class CollectionListAdapter(data: MutableList<SubjectCollection>? = null) :
        BaseQuickAdapter<SubjectCollection, BaseViewHolder>(R.layout.item_subject, data) {

    override fun convert(helper: BaseViewHolder, item: SubjectCollection) {
        helper.setText(R.id.item_title, item.subject?.getPrettyName())
        helper.setText(R.id.item_name_jp, item.subject?.name)
        val eps = (item.subject?.eps as? List<*>)?.mapNotNull { it as? Episode }
        val watchTo = eps?.lastOrNull { it.progress?.status?.id == SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH_ID }
        val airTo = eps?.lastOrNull { it.status == "Air" }
        helper.itemView.item_summary.setTextColor(ResourceUtil.resolveColorAttr(helper.itemView.context,
                if (watchTo != airTo)R.attr.colorPrimary
                    else android.R.attr.textColorSecondary
                ))
        val watchep = (watchTo?.parseSort()?.let{ "看到$it"}?:"尚未观看") +
                if(eps?.any { it.status != "Air" } == true) airTo?.parseSort()?.let{ " / 更新到$it"  }?:"" else if(item.subject?.eps_count?:0 > 0) " / 全 ${item.subject?.eps_count} 话" else ""
        helper.setText(R.id.item_summary, if(item.ep_status == -1) item.subject?.summary else watchep)
        Glide.with(helper.itemView.item_cover)
                .load(item.subject?.images?.getImage(helper.itemView.context))
                .apply(RequestOptions.errorOf(R.drawable.ic_404))
                .into(helper.itemView.item_cover)
    }
}