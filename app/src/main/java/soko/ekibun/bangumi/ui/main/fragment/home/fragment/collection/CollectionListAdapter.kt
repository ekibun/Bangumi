package soko.ekibun.bangumi.ui.main.fragment.home.fragment.collection

import com.bumptech.glide.request.RequestOptions
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.module.LoadMoreModule
import com.chad.library.adapter.base.viewholder.BaseViewHolder
import kotlinx.android.synthetic.main.item_subject.view.*
import soko.ekibun.bangumi.R
import soko.ekibun.bangumi.api.bangumi.bean.Episode
import soko.ekibun.bangumi.api.bangumi.bean.Images
import soko.ekibun.bangumi.api.bangumi.bean.Subject
import soko.ekibun.bangumi.util.GlideUtil
import soko.ekibun.bangumi.util.ResourceUtil

/**
 * 收藏列表Adapter
 * @constructor
 */
class CollectionListAdapter(data: MutableList<Subject>? = null) :
    BaseQuickAdapter<Subject, BaseViewHolder>(R.layout.item_subject, data), LoadMoreModule {

    override fun convert(holder: BaseViewHolder, item: Subject) {
        holder.setText(R.id.item_title, item.displayName)
        holder.setText(R.id.item_name_jp, item.name)

        holder.itemView.item_onair.text = item.airInfo

        holder.setText(R.id.item_summary, when {
            item.ep_status == -1 -> {
                holder.itemView.item_summary.setTextColor(
                    ResourceUtil.resolveColorAttr(
                        holder.itemView.context,
                        android.R.attr.textColorSecondary
                    )
                )
                item.summary
            }
            item.type == Subject.TYPE_BOOK -> {
                val context = holder.itemView.context
                holder.itemView.item_summary.setTextColor(
                    ResourceUtil.resolveColorAttr(
                        holder.itemView.context,
                        if (item.airInfo.isNullOrEmpty()) android.R.attr.textColorSecondary
                        else R.attr.colorPrimary
                    )
                )
                context.getString(
                    R.string.phrase_progress,
                    (if (item.vol_count != 0) context.getString(
                        R.string.parse_sort_vol,
                        "${item.vol_status}${if (item.vol_count <= 0) "" else "/${item.vol_count}"}"
                    ) + " " else "") + context.getString(
                        R.string.parse_sort_ep,
                        "${item.ep_status}${if (item.eps_count <= 0) "" else "/${item.eps_count}"}"
                    )
                )
            }
            else -> {
                val eps = (item.eps as? List<*>)?.mapNotNull { it as? Episode }?.filter { it.type == Episode.TYPE_MAIN }
                val watchTo = eps?.lastOrNull { it.progress == Episode.PROGRESS_WATCH }
                val airTo = eps?.lastOrNull { it.isAir }
                holder.itemView.item_summary.setTextColor(
                    ResourceUtil.resolveColorAttr(
                        holder.itemView.context,
                        if (watchTo != airTo) R.attr.colorPrimary
                        else android.R.attr.textColorSecondary
                    )
                )
                (watchTo?.parseSort()?.let { holder.itemView.context.getString(R.string.parse_watch_to, it) }
                    ?: holder.itemView.context.getString(R.string.hint_watch_nothing)) +
                        when {
                            eps?.any { !it.isAir } == true -> airTo?.parseSort()
                                ?.let { " / " + holder.itemView.context.getString(R.string.parse_update_to, it) } ?: ""
                            item.eps_count > 0 -> " / " + holder.itemView.context.getString(
                                R.string.phrase_full_eps,
                                item.eps_count
                            )
                            else -> ""
                        }
            }
        })
        GlideUtil.with(holder.itemView.item_cover)
            ?.load(Images.getImage(item.image))
            ?.apply(RequestOptions.errorOf(R.drawable.err_404).placeholder(R.drawable.placeholder))
            ?.into(holder.itemView.item_cover)
    }
}