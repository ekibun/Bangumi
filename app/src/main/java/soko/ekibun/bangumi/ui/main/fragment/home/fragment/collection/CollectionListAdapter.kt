package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.SubjectCollection
import soko.ekibun.bangumi.api.bangumi.bean.SubjectProgress
import soko.ekibun.bangumi.api.bangumi.bean.SubjectType
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil

class CollectionListAdapter(data: MutableList<SubjectCollection>? = null) :
        BaseQuickAdapter<SubjectCollection, BaseViewHolder>(R.layout.item_subject, data) {

    override fun convert(helper: BaseViewHolder, item: SubjectCollection) {
        helper.setText(R.id.item_title, item.subject?.getPrettyName())
        helper.setText(R.id.item_name_jp, item.subject?.name)
        val watchep = if(item.subject?.type == SubjectType.BOOK){
            val context = helper.itemView.context
            helper.itemView.item_summary.setTextColor(ResourceUtil.resolveColorAttr(helper.itemView.context,
                    if (item.subject?.has_vol == true && (item.subject?.vol_count == 0 || item.subject?.vol_status != item.subject?.vol_count) || item.subject?.eps_count == 0 || item.subject?.ep_status != item.subject?.eps_count) R.attr.colorPrimary
                    else android.R.attr.textColorSecondary
            ))
            context.getString(R.string.phrase_progress,
                    (if(item.subject?.has_vol == true) context.getString(R.string.parse_sort_vol, "${item.subject?.vol_status}${ if(item.subject?.vol_count == 0) "" else "/${item.subject?.vol_count}" }") + " " else "") +
                            context.getString(R.string.parse_sort_ep, "${item.subject?.ep_status}${ if(item.subject?.eps_count == 0) "" else "/${item.subject?.eps_count}"}"))
        }else{
            val eps = (item.subject?.eps as? List<*>)?.mapNotNull { it as? Episode }
            val watchTo = eps?.lastOrNull { it.progress?.status?.id == SubjectProgress.EpisodeProgress.EpisodeStatus.WATCH_ID }
            val airTo = eps?.lastOrNull { it.status == "Air" }
            helper.itemView.item_summary.setTextColor(ResourceUtil.resolveColorAttr(helper.itemView.context,
                    if (watchTo != airTo)R.attr.colorPrimary
                    else android.R.attr.textColorSecondary
            ))
            (watchTo?.parseSort(helper.itemView.context)?.let{ helper.itemView.context.getString(R.string.parse_watch_to, it) }?: helper.itemView.context.getString(R.string.hint_watch_nothing)) +
                    when {
                        eps?.any { it.status != "Air" } == true -> airTo?.parseSort(helper.itemView.context)?.let{ " / " + helper.itemView.context.getString(R.string.parse_update_to, it)  }?:""
                        item.subject?.eps_count?:0 > 0 -> " / "+ helper.itemView.context.getString(R.string.phrase_full_eps, item.subject?.eps_count)
                        else -> ""
                    }
        }
        helper.setText(R.id.item_summary, if(item.ep_status == -1) item.subject?.summary else watchep)
        GlideUtil.with(helper.itemView.item_cover)
                ?.load(item.subject?.images?.getImage(helper.itemView.context))
                ?.apply(RequestOptions.errorOf(R.drawable.err_404))
                ?.into(helper.itemView.item_cover)
    }
}